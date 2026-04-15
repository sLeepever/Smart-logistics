<template>
  <div class="vehicle-view app-page">
    <el-card class="vehicle-view__panel app-console-panel" shadow="never">
      <template #header>
        <div class="vehicle-view__header">
          <div>
            <span class="vehicle-view__title">车辆工作台</span>
            <p class="vehicle-view__subtitle">查看车辆状态、编辑运力参数、切换维修状态和绑定司机。</p>
          </div>
          <el-button type="primary" :icon="Plus" @click="openCreate">新增车辆</el-button>
        </div>
      </template>

      <el-form inline class="vehicle-view__toolbar app-toolbar-panel">
        <el-form-item label="状态">
          <el-select v-model="filterStatus" placeholder="全部" clearable style="width: 140px" @change="load">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" border stripe class="vehicle-view__table">
      <el-table-column prop="plateNo" label="车牌号" width="120" />
      <el-table-column prop="vehicleType" label="车型" width="100" />
      <el-table-column prop="maxWeight" label="载重(kg)" width="100" align="right" />
      <el-table-column prop="maxVolume" label="容积(m³)" width="100" align="right" />
      <el-table-column prop="driverId" label="绑定司机" width="130" align="center">
        <template #default="{ row }">
          {{ driverLabel(row.driverId) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button
            v-if="row.status === 'maintenance'"
            link type="success" size="small"
            @click="changeStatus(row.id, 'idle')"
          >设为空闲</el-button>
          <el-button
            v-if="row.status === 'idle'"
            link type="warning" size="small"
            @click="changeStatus(row.id, 'maintenance')"
          >送修</el-button>
          <el-popconfirm
            v-if="row.status !== 'on_route'"
            title="确认删除该车辆？"
            @confirm="handleDelete(row.id)"
          >
            <template #reference>
              <el-button link type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

      <el-pagination
      class="vehicle-view__pagination"
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      @change="load"
    />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑车辆' : '新增车辆'" width="520px" class="vehicle-view__dialog">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="车牌号" prop="plateNo">
        <el-input v-model="form.plateNo" />
      </el-form-item>
      <el-form-item label="车型" prop="vehicleType">
        <el-select v-model="form.vehicleType" style="width:100%">
          <el-option label="厢式货车" value="厢式货车" />
          <el-option label="冷藏车" value="冷藏车" />
          <el-option label="重型卡车" value="重型卡车" />
        </el-select>
      </el-form-item>
      <el-form-item label="载重(kg)" prop="maxWeight">
        <el-input-number v-model="form.maxWeight" :min="100" style="width:100%" />
      </el-form-item>
      <el-form-item label="容积(m³)" prop="maxVolume">
        <el-input-number v-model="form.maxVolume" :min="1" :precision="2" style="width:100%" />
      </el-form-item>
      <el-form-item label="绑定司机">
        <el-select
          v-model="form.driverId"
          placeholder="选择司机（可不绑定）"
          clearable
          style="width:100%"
          :loading="driversLoading"
        >
          <el-option
            v-for="d in drivers"
            :key="d.id"
            :label="`${d.username}${d.realName ? '（' + d.realName + '）' : ''}`"
            :value="d.id"
          />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import { vehicleApi, type Vehicle } from '@/api/vehicle'
import { userApi, type User } from '@/api/user'

const statusOptions = [
  { value: 'idle',        label: '空闲',  type: 'success' as const },
  { value: 'on_route',    label: '在途',  type: 'warning' as const },
  { value: 'maintenance', label: '维修中', type: 'danger'  as const },
]
const statusLabel = (s: string) => statusOptions.find(o => o.value === s)?.label ?? s
const statusType  = (s: string) => statusOptions.find(o => o.value === s)?.type ?? ''

const loading = ref(false)
const tableData = ref<Vehicle[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filterStatus = ref('')

// 司机列表（仅 driver 角色，用于下拉选择）
const drivers = ref<User[]>([])
const driversLoading = ref(false)

async function loadDrivers() {
  driversLoading.value = true
  try {
    const res = await userApi.list({ page: 1, size: 100, role: 'driver' })
    drivers.value = res.data.records
  } catch {
    // 静默失败，不影响车辆管理主功能
  } finally {
    driversLoading.value = false
  }
}

function driverLabel(driverId: number | null) {
  if (!driverId) return '未绑定'
  const d = drivers.value.find(u => u.id === driverId)
  return d ? d.username : `ID: ${driverId}`
}

async function load() {
  loading.value = true
  try {
    const res = await vehicleApi.list({ page: page.value, size: size.value, status: filterStatus.value })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}
onMounted(() => {
  load()
  loadDrivers()
})

async function changeStatus(id: number, status: string) {
  await vehicleApi.changeStatus(id, status)
  ElMessage.success('状态已更新')
  load()
}
async function handleDelete(id: number) {
  await vehicleApi.delete(id)
  ElMessage.success('删除成功')
  load()
}

const dialogVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({ plateNo: '', vehicleType: '厢式货车', maxWeight: 2000, maxVolume: 12, driverId: undefined as number | undefined })
const rules = {
  plateNo:     [{ required: true, message: '请输入车牌号', trigger: 'blur' }],
  vehicleType: [{ required: true, message: '请选择车型',   trigger: 'change' }],
  maxWeight:   [{ required: true, message: '请输入载重',   trigger: 'blur' }],
  maxVolume:   [{ required: true, message: '请输入容积',   trigger: 'blur' }],
}

function openCreate() {
  editingId.value = null
  Object.assign(form, { plateNo: '', vehicleType: '厢式货车', maxWeight: 2000, maxVolume: 12, driverId: undefined })
  dialogVisible.value = true
}
function openEdit(row: Vehicle) {
  editingId.value = row.id
  Object.assign(form, { plateNo: row.plateNo, vehicleType: row.vehicleType, maxWeight: row.maxWeight, maxVolume: row.maxVolume, driverId: row.driverId })
  dialogVisible.value = true
}
async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (editingId.value) {
      await vehicleApi.update(editingId.value, form)
      ElMessage.success('更新成功')
    } else {
      await vehicleApi.create({ ...form, status: 'idle' })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.vehicle-view__panel {
  position: relative;
}

.vehicle-view__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--app-space-4);
}

.vehicle-view__title {
  color: var(--app-text-strong);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.vehicle-view__subtitle {
  margin-top: 6px;
  color: var(--app-text-secondary);
  line-height: 1.7;
}

.vehicle-view__toolbar {
  margin-bottom: var(--app-space-4);
}

.vehicle-view__table {
  width: 100%;
}

.vehicle-view__pagination {
  justify-content: flex-end;
  margin-top: var(--app-space-4);
}

@media (max-width: 768px) {
  .vehicle-view__header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
