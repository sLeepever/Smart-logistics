<template>
  <el-card>
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span style="font-weight: 600">订单管理</span>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建订单</el-button>
      </div>
    </template>

    <!-- 筛选栏 -->
    <el-form :model="query" inline style="margin-bottom: 12px">
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="日期">
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 240px"
        />
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="订单号/收货人/电话" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" border stripe>
      <el-table-column prop="orderNo" label="订单号" width="160" />
      <el-table-column prop="receiverName" label="收货人" width="90" />
      <el-table-column prop="receiverPhone" label="联系电话" width="120" />
      <el-table-column prop="receiverAddress" label="收货地址" min-width="160" show-overflow-tooltip />
      <el-table-column prop="goodsName" label="货物" width="100" show-overflow-tooltip />
      <el-table-column prop="weight" label="重量(kg)" width="90" align="right" />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="canChangeStatus(row)"
            link type="primary" size="small"
            @click="openStatusChange(row)"
          >变更状态</el-button>
          <el-button
            v-if="row.status === 'pending'"
            link type="warning" size="small"
            @click="openEdit(row)"
          >编辑</el-button>
          <el-popconfirm
            v-if="row.status === 'pending'"
            title="确认删除该订单？"
            @confirm="handleDelete(row.id)"
          >
            <template #reference>
              <el-button link type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      style="margin-top: 16px; justify-content: flex-end"
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      @change="loadData"
    />
  </el-card>

  <!-- 新建/编辑订单弹窗 -->
  <el-dialog v-model="formVisible" :title="editingId ? '编辑订单' : '新建订单'" width="560px" @close="resetForm">
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
      <el-form-item label="收货人" prop="receiverName">
        <el-input v-model="form.receiverName" />
      </el-form-item>
      <el-form-item label="联系电话" prop="receiverPhone">
        <el-input v-model="form.receiverPhone" />
      </el-form-item>
      <el-form-item label="收货地址" prop="receiverAddress">
        <el-input v-model="form.receiverAddress" />
      </el-form-item>
      <el-form-item label="货物名称" prop="goodsName">
        <el-input v-model="form.goodsName" />
      </el-form-item>
      <el-form-item label="重量(kg)" prop="weight">
        <el-input-number v-model="form.weight" :min="0.1" :precision="2" style="width: 100%" />
      </el-form-item>
      <el-form-item label="体积(m³)">
        <el-input-number v-model="form.volume" :min="0" :precision="3" style="width: 100%" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="formVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>

  <!-- 状态变更弹窗 -->
  <el-dialog v-model="statusVisible" title="变更订单状态" width="400px">
    <el-form label-width="80px">
      <el-form-item label="当前状态">
        <el-tag :type="statusType(statusRow?.status)">{{ statusLabel(statusRow?.status) }}</el-tag>
      </el-form-item>
      <el-form-item label="目标状态">
        <el-select v-model="targetStatus" style="width: 100%">
          <el-option
            v-for="s in allowedTargets"
            :key="s.value" :label="s.label" :value="s.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="statusRemark" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="statusVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleStatusChange">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import { orderApi, type Order } from '@/api/order'

// ---- 状态映射 ----
const statusOptions = [
  { value: 'pending',     label: '待调度' },
  { value: 'dispatched',  label: '已调度' },
  { value: 'in_progress', label: '配送中' },
  { value: 'completed',   label: '已完成' },
  { value: 'cancelled',   label: '已取消' },
  { value: 'exception',   label: '异常'   },
]

const STATUS_TRANSITIONS: Record<string, string[]> = {
  pending:     ['dispatched', 'cancelled'],
  dispatched:  ['in_progress', 'cancelled', 'pending'],
  in_progress: ['completed', 'exception'],
  exception:   ['pending', 'cancelled'],
}

function statusLabel(status?: string) {
  return statusOptions.find(s => s.value === status)?.label ?? status ?? ''
}

function statusType(status?: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    pending: '',
    dispatched: 'warning',
    in_progress: 'success',
    completed: 'success',
    cancelled: 'info',
    exception: 'danger',
  }
  return map[status ?? ''] ?? ''
}

function formatTime(t: string) {
  return t ? t.replace('T', ' ').substring(0, 16) : ''
}

// ---- 列表 ----
const loading = ref(false)
const tableData = ref<Order[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)

const query = reactive({
  page: 1,
  size: 10,
  status: '',
  keyword: '',
  startDate: '',
  endDate: '',
})

async function loadData() {
  loading.value = true
  try {
    const params = {
      ...query,
      startDate: dateRange.value?.[0] ?? '',
      endDate: dateRange.value?.[1] ?? '',
    }
    const res = await orderApi.list(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadData()
}

function handleReset() {
  query.status = ''
  query.keyword = ''
  query.page = 1
  dateRange.value = null
  loadData()
}

function canChangeStatus(row: Order) {
  return !!STATUS_TRANSITIONS[row.status]?.length
}

async function handleDelete(id: number) {
  await orderApi.delete(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)

// ---- 新建/编辑 ----
const formVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  goodsName: '',
  weight: 1,
  volume: 0,
  remark: '',
})

const formRules = {
  receiverName:    [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone:   [{ required: true, message: '请输入联系电话',   trigger: 'blur' }],
  receiverAddress: [{ required: true, message: '请输入收货地址',   trigger: 'blur' }],
  goodsName:       [{ required: true, message: '请输入货物名称',   trigger: 'blur' }],
  weight:          [{ required: true, message: '请输入重量',       trigger: 'blur' }],
}

function openCreate() {
  editingId.value = null
  formVisible.value = true
}

function openEdit(row: Order) {
  editingId.value = row.id
  Object.assign(form, {
    receiverName: row.receiverName,
    receiverPhone: row.receiverPhone,
    receiverAddress: row.receiverAddress,
    goodsName: row.goodsName,
    weight: row.weight,
    volume: row.volume,
    remark: row.remark,
  })
  formVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  Object.assign(form, { receiverName: '', receiverPhone: '', receiverAddress: '', goodsName: '', weight: 1, volume: 0, remark: '' })
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload = {
      ...form,
      senderName: '广工仓储中心',
      senderPhone: '02039322000',
      senderAddress: '广东工业大学大学城校区仓库',
      senderLng: 113.396,
      senderLat: 23.0452,
      receiverLng: 0,
      receiverLat: 0,
    }
    if (editingId.value) {
      await orderApi.update(editingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await orderApi.create(payload)
      ElMessage.success('创建成功')
    }
    formVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

// ---- 状态变更 ----
const statusVisible = ref(false)
const statusRow = ref<Order | null>(null)
const targetStatus = ref('')
const statusRemark = ref('')

const allowedTargets = computed(() =>
  (STATUS_TRANSITIONS[statusRow.value?.status ?? ''] ?? []).map(v => ({
    value: v,
    label: statusLabel(v),
  })),
)

function openStatusChange(row: Order) {
  statusRow.value = row
  targetStatus.value = allowedTargets.value[0]?.value ?? ''
  statusRemark.value = ''
  statusVisible.value = true
}

async function handleStatusChange() {
  if (!targetStatus.value) return
  submitting.value = true
  try {
    await orderApi.changeStatus(statusRow.value!.id, targetStatus.value, statusRemark.value)
    ElMessage.success('状态变更成功')
    statusVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}
</script>
