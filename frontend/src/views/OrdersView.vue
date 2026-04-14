<template>
  <div class="orders-view app-page">
    <el-card class="orders-view__panel app-console-panel" shadow="never">
    <template #header>
      <div class="orders-view__header">
        <div>
          <span class="orders-view__title">订单工作区</span>
          <p class="orders-view__subtitle">筛选、审核、变更状态与查看订单详情都集中在同一页面完成。</p>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建订单</el-button>
      </div>
    </template>

    <!-- 筛选栏 -->
     <el-form :model="query" inline class="orders-view__toolbar app-toolbar-panel">
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
          <el-input v-model="query.keyword" placeholder="订单号/收货人/货物名称/电话" clearable style="width: 220px" />
        </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" border stripe class="orders-view__table">
      <el-table-column prop="orderNo" label="订单号" width="160">
        <template #default="{ row }">
          <el-badge
            :value="chatUnread.getCount(row.id)"
            :max="99"
            :hidden="chatUnread.getCount(row.id) === 0"
            class="orders-view__chat-badge"
          >
            {{ row.orderNo }}
          </el-badge>
        </template>
      </el-table-column>
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
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              link type="primary" size="small"
              @click="openDetail(row)"
            >详情</el-button>
            <el-button
              v-if="canChangeStatus(row)"
              link type="primary" size="small"
             @click="openStatusChange(row)"
          >{{ row.status === 'pending_review' ? '审核订单' : '变更状态' }}</el-button>
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
      class="orders-view__pagination"
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      @change="loadData"
    />
    </el-card>

    <!-- 新建/编辑订单弹窗 -->
    <el-dialog
      v-model="formVisible"
      :title="editingId ? '编辑订单' : '新建订单'"
      width="560px"
      class="orders-view__dialog"
      @close="resetForm"
    >
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
      <el-form-item label="收货人" prop="receiverName">
        <el-input v-model="form.receiverName" />
      </el-form-item>
      <el-form-item label="联系电话" prop="receiverPhone">
        <el-input v-model="form.receiverPhone" />
      </el-form-item>
      <el-form-item label="收货地址" prop="receiverAddress">
        <div style="display:flex;gap:8px;width:100%">
          <el-input v-model="form.receiverAddress" style="flex:1" />
          <el-button :loading="geocoding" @click="handleGeocode">查询坐标</el-button>
        </div>
      </el-form-item>
      <el-form-item label="经纬度">
        <div style="display:flex;gap:8px;width:100%">
          <el-input-number v-model="form.receiverLng" :precision="6" :step="0.001" style="flex:1" placeholder="经度" />
          <el-input-number v-model="form.receiverLat" :precision="6" :step="0.001" style="flex:1" placeholder="纬度" />
        </div>
      </el-form-item>
      <el-form-item label="货物名称" prop="goodsName">
        <el-input v-model="form.goodsName" />
      </el-form-item>
      <el-form-item label="重量(kg)" prop="weight">
        <el-input-number v-model="form.weight" :min="0.1" :precision="2" style="width:100%" />
      </el-form-item>
      <el-form-item label="体积(m³)">
        <el-input-number v-model="form.volume" :min="0" :precision="3" style="width:100%" />
      </el-form-item>
      <!-- 创建人联系信息（选填） -->
      <el-divider content-position="left" style="margin:8px 0 4px">创建人联系信息（选填）</el-divider>
      <el-form-item label="创建人姓名">
        <div style="display:flex;gap:8px;width:100%">
          <el-input
            v-model="form.creatorName"
            :disabled="!creatorInfoEnabled"
            :placeholder="creatorInfoEnabled ? '请输入姓名' : '默认使用当前账号信息'"
            style="flex:1"
          />
          <el-button v-if="!creatorInfoEnabled" @click="enableCreatorInfo">填入我的信息</el-button>
          <el-button v-else type="info" plain @click="clearCreatorInfo">清除</el-button>
        </div>
      </el-form-item>
      <el-form-item label="创建人电话">
        <el-input
          v-model="form.creatorPhone"
          :disabled="!creatorInfoEnabled"
          :placeholder="creatorInfoEnabled ? '请输入联系电话' : '默认使用当前账号信息'"
        />
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
    <el-dialog v-model="statusVisible" :title="statusDialogTitle" width="400px" class="orders-view__dialog">
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

    <el-dialog v-model="detailVisible" title="订单详情" width="880px" class="orders-view__dialog orders-view__detail-dialog">
    <div v-loading="detailLoading" class="orders-view__detail">
      <template v-if="detailOrder">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detailOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detailOrder.status)" size="small">{{ statusLabel(detailOrder.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="寄件人">{{ detailOrder.senderName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="寄件电话">{{ detailOrder.senderPhone || '--' }}</el-descriptions-item>
          <el-descriptions-item label="寄件地址" :span="2">{{ detailOrder.senderAddress || '--' }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ detailOrder.receiverName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="收货电话">{{ detailOrder.receiverPhone || '--' }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{ detailOrder.receiverAddress || '--' }}</el-descriptions-item>
          <el-descriptions-item label="货物名称">{{ detailOrder.goodsName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="重量/体积">
            {{ detailOrder.weight }} kg / {{ detailOrder.volume || 0 }} m³
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailOrder.remark || '--' }}</el-descriptions-item>
          <el-descriptions-item label="创建人">
            <span v-if="detailCreator">
              {{ detailCreator.realName || detailCreator.username }}
              <el-text type="info" size="small" style="margin-left:6px">{{ detailCreator.phone || '' }}</el-text>
            </span>
            <span v-else class="orders-view__creator-loading">--</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(detailOrder.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatTime(detailOrder.updatedAt) }}</el-descriptions-item>
        </el-descriptions>

        <OrderChatPanel
          :order-id="detailOrder.id"
          class="orders-view__chat"
          panel-title="订单沟通"
        />
      </template>
    </div>

    <template #footer>
      <el-button @click="detailVisible = false">关闭</el-button>
    </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import OrderChatPanel from '@/components/OrderChatPanel.vue'
import { orderApi, type Order } from '@/api/order'
import { userApi, type UserBrief } from '@/api/user'
import { useAuthStore } from '@/stores/auth'
import { useGeocode } from '@/composables/useGeocode'
import { useChatUnreadStore } from '@/stores/chatUnread'

const { geocode, geocoding } = useGeocode()
const chatUnread = useChatUnreadStore()
const authStore = useAuthStore()

// ---- 状态映射 ----
const statusOptions = [
  { value: 'pending_review', label: '待审核' },
  { value: 'pending',     label: '待调度' },
  { value: 'dispatched',  label: '已调度' },
  { value: 'in_progress', label: '配送中' },
  { value: 'completed',   label: '已完成' },
  { value: 'cancelled',   label: '已取消' },
  { value: 'exception',   label: '异常'   },
]

const STATUS_TRANSITIONS: Record<string, string[]> = {
  pending_review: ['pending', 'cancelled'],
  pending:     ['dispatched', 'cancelled'],
  dispatched:  ['in_progress', 'cancelled'],
  in_progress: ['completed', 'exception'],
  exception:   ['pending', 'cancelled'],
}

function statusLabel(status?: string) {
  return statusOptions.find(s => s.value === status)?.label ?? status ?? ''
}

function statusType(status?: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    pending_review: 'warning',
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
    chatUnread.fetchCounts(tableData.value.map(o => o.id))
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
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailOrder = ref<Order | null>(null)
const detailCreator = ref<UserBrief | null>(null)

const form = reactive({
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  receiverLng: 0,
  receiverLat: 0,
  goodsName: '',
  weight: 1,
  volume: 0,
  remark: '',
  creatorName: '',
  creatorPhone: '',
})

const creatorInfoEnabled = ref(false)

function enableCreatorInfo() {
  creatorInfoEnabled.value = true
  form.creatorName = authStore.userInfo.realName || authStore.userInfo.username
  form.creatorPhone = authStore.userInfo.phone
}

function clearCreatorInfo() {
  creatorInfoEnabled.value = false
  form.creatorName = ''
  form.creatorPhone = ''
}

const formRules = {
  receiverName:    [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone:   [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^\d{10}$/, message: '电话号码必须为10位数字', trigger: 'blur' },
  ],
  receiverAddress: [{ required: true, message: '请输入收货地址',   trigger: 'blur' }],
  receiverLng:     [{ required: true, type: 'number' as const, min: 0.000001, message: '请输入有效收货经度', trigger: 'blur' }],
  receiverLat:     [{ required: true, type: 'number' as const, min: 0.000001, message: '请输入有效收货纬度', trigger: 'blur' }],
  goodsName:       [{ required: true, message: '请输入货物名称',   trigger: 'blur' }],
  weight:          [{ required: true, message: '请输入重量',       trigger: 'blur' }],
}

function openCreate() {
  editingId.value = null
  formVisible.value = true
}

async function fetchOrder(id: number) {
  const res = await orderApi.getById(id)
  return res.data
}

async function openDetail(row: Order) {
  detailVisible.value = true
  detailLoading.value = true
  detailOrder.value = null
  detailCreator.value = null
  try {
    detailOrder.value = await fetchOrder(row.id)
    if (detailOrder.value?.creatorId) {
      try {
        const res = await userApi.getBrief(detailOrder.value.creatorId)
        detailCreator.value = res.data
      } catch {
        // 获取创建人信息失败时静默处理
      }
    }
  } finally {
    detailLoading.value = false
  }
}

function openEdit(row: Order) {
  editingId.value = row.id
  creatorInfoEnabled.value = !!(row.senderName && row.senderName !== '广工仓储中心')
  Object.assign(form, {
    receiverName: row.receiverName,
    receiverPhone: row.receiverPhone,
    receiverAddress: row.receiverAddress,
    receiverLng: row.receiverLng ?? 0,
    receiverLat: row.receiverLat ?? 0,
    goodsName: row.goodsName,
    weight: row.weight,
    volume: row.volume,
    remark: row.remark,
    creatorName: (row.senderName && row.senderName !== '广工仓储中心') ? row.senderName : '',
    creatorPhone: (row.senderPhone && row.senderPhone !== '02039322000') ? row.senderPhone : '',
  })
  formVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  creatorInfoEnabled.value = false
  Object.assign(form, { receiverName: '', receiverPhone: '', receiverAddress: '', receiverLng: 0, receiverLat: 0, goodsName: '', weight: 1, volume: 0, remark: '', creatorName: '', creatorPhone: '' })
}

async function handleGeocode() {
  if (!form.receiverAddress?.trim()) {
    ElMessage.warning('请先输入收货地址')
    return
  }
  const result = await geocode(form.receiverAddress)
  if (result) {
    form.receiverLng = result.lng
    form.receiverLat = result.lat
    ElMessage.success(`坐标已填充：${result.lng}, ${result.lat}`)
  } else {
    ElMessage.error('地址解析失败，请检查地址或手动输入坐标')
  }
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  const currentEditingId = editingId.value
  try {
    const payload = {
      ...form,
      senderName: form.creatorName || '广工仓储中心',
      senderPhone: form.creatorPhone || '02039322000',
      senderAddress: '广东工业大学大学城校区仓库',
      senderLng: 113.396,
      senderLat: 23.0452,
      receiverLng: form.receiverLng,
      receiverLat: form.receiverLat,
    }
    if (currentEditingId) {
      await orderApi.update(currentEditingId, payload)
      ElMessage.success('更新成功')
    } else {
      await orderApi.create(payload)
      ElMessage.success('创建成功')
    }
    formVisible.value = false
    loadData()

    if (currentEditingId && detailOrder.value?.id === currentEditingId) {
      detailOrder.value = await fetchOrder(currentEditingId)
    }
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

const statusDialogTitle = computed(() =>
  statusRow.value?.status === 'pending_review' ? '审核订单' : '变更订单状态',
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
  const currentStatusRow = statusRow.value
  try {
    if (!currentStatusRow) {
      return
    }

    if (currentStatusRow.status === 'pending_review') {
      const action = targetStatus.value === 'pending'
        ? 'approve'
        : targetStatus.value === 'cancelled'
          ? 'reject'
          : ''

      if (!action) {
        ElMessage.error('待审核订单仅支持审核通过或驳回')
        return
      }

      await orderApi.review(currentStatusRow.id, action, statusRemark.value)
      ElMessage.success(action === 'approve' ? '审核通过' : '已驳回')
    } else {
      await orderApi.changeStatus(currentStatusRow.id, targetStatus.value, statusRemark.value)
      ElMessage.success('状态变更成功')
    }
    statusVisible.value = false
    loadData()

    if (detailOrder.value?.id === currentStatusRow?.id) {
      detailOrder.value = await fetchOrder(currentStatusRow.id)
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.orders-view__chat-badge :deep(.el-badge__content) {
  font-size: 10px;
  height: 16px;
  line-height: 16px;
  min-width: 16px;
  padding: 0 4px;
}


.orders-view__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--app-space-4);
}

.orders-view__title {
  display: block;
  color: var(--app-text-strong);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.orders-view__subtitle {
  margin-top: 6px;
  color: var(--app-text-secondary);
  font-size: 13px;
}

.orders-view__toolbar {
  align-items: end;
}

.orders-view__table {
  width: 100%;
}

.orders-view__pagination {
  margin-top: var(--app-space-4);
  justify-content: flex-end;
}

.orders-view__detail {
  min-height: 220px;
}

.orders-view__chat {
  margin-top: var(--app-space-4);
}

.orders-view__detail :deep(.el-descriptions) {
  margin-bottom: 0;
}

@media (max-width: 768px) {
  .orders-view__header {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
