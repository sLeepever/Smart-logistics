<template>
  <div class="customer-home app-page" data-testid="customer-order-center">
    <el-card class="customer-home__card customer-home__hero app-console-panel" shadow="never">
      <template #header>
        <div class="customer-home__header">
          <div>
            <div class="customer-home__eyebrow">客户订单中心</div>
            <div class="customer-home__title-row">
              <span class="customer-home__title">我的订单</span>
              <el-tag type="warning" effect="light">待审核订单可修改</el-tag>
            </div>
          </div>
          <el-button
            type="primary"
            data-testid="customer-create-order-button"
            @click="openCreate"
          >
            新建订单
          </el-button>
        </div>
      </template>

      <div class="customer-home__hero-content">
        <div>
          <p class="customer-home__description">
            您好，{{ displayName }}。这里仅展示当前客户账号创建的订单，所有新订单都会先进入
            <strong>待审核</strong>
            ，审核通过后才进入内部调度流程。
          </p>
          <el-alert
            title="客户可在待审核阶段修改或取消订单；进入待调度及后续状态后仅支持查看详情。"
            type="info"
            :closable="false"
            show-icon
          />
        </div>

        <div class="customer-home__metrics">
          <div class="customer-home__metric">
            <span class="customer-home__metric-label">订单总数</span>
            <strong class="customer-home__metric-value">{{ total }}</strong>
          </div>
          <div class="customer-home__metric">
            <span class="customer-home__metric-label">当前筛选</span>
            <strong class="customer-home__metric-value customer-home__metric-value--small">
              {{ currentStatusLabel }}
            </strong>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="customer-home__card app-console-panel" shadow="never">
      <template #header>
        <div class="customer-home__header">
          <span class="customer-home__section-title">订单列表</span>
          <el-button @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-form
        :model="query"
        inline
        class="customer-home__toolbar app-toolbar-panel"
        data-testid="customer-order-filter-form"
      >
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable class="customer-home__field--sm">
            <el-option
              v-for="option in statusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="订单号/收货人/货物名称"
            clearable
            class="customer-home__field--md"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            data-testid="customer-order-search-button"
            @click="handleSearch"
          >
            查询
          </el-button>
          <el-button data-testid="customer-order-reset-button" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        class="customer-home__table"
        data-testid="customer-order-table"
      >
        <el-table-column prop="orderNo" label="订单号" min-width="170" />
        <el-table-column prop="receiverName" label="收货人" width="110" />
        <el-table-column prop="receiverPhone" label="联系电话" width="140" />
        <el-table-column prop="receiverAddress" label="收货地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="goodsName" label="货物" min-width="140" show-overflow-tooltip />
        <el-table-column label="重量/体积" width="140" align="right">
          <template #default="{ row }">
            <span>{{ row.weight }} kg / {{ row.volume || 0 }} m3</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              :data-testid="`customer-order-detail-${row.id}`"
              @click="openDetail(row)"
            >
              详情
            </el-button>
            <el-button
              v-if="isPendingReview(row)"
              link
              type="warning"
              size="small"
              :data-testid="`customer-order-edit-${row.id}`"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="isPendingReview(row)"
              link
              type="danger"
              size="small"
              :data-testid="`customer-order-cancel-${row.id}`"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
            <span v-else class="customer-home__action-hint">审核完成后不可修改</span>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty :description="emptyDescription" :image-size="72">
            <el-button type="primary" @click="openCreate">立即创建订单</el-button>
          </el-empty>
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        class="customer-home__pagination"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="loadData"
      />
    </el-card>

    <el-dialog
      v-model="formVisible"
      :title="editingId ? '编辑订单' : '新建订单'"
      width="760px"
      data-testid="customer-order-form-dialog"
      @closed="handleFormClosed"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="92px"
        class="customer-home__form"
      >
        <div class="customer-home__form-grid">
          <el-form-item label="寄件人" prop="senderName">
            <el-input v-model="form.senderName" />
          </el-form-item>
          <el-form-item label="寄件电话" prop="senderPhone">
            <el-input v-model="form.senderPhone" />
          </el-form-item>
          <el-form-item label="寄件地址" prop="senderAddress" class="customer-home__form-span-2">
            <el-input v-model="form.senderAddress" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="收货人" prop="receiverName">
            <el-input v-model="form.receiverName" />
          </el-form-item>
          <el-form-item label="收货电话" prop="receiverPhone">
            <el-input v-model="form.receiverPhone" />
          </el-form-item>
          <el-form-item label="收货地址" prop="receiverAddress" class="customer-home__form-span-2">
            <div style="display:flex;gap:8px;width:100%">
              <el-input v-model="form.receiverAddress" type="textarea" :rows="2" style="flex:1" />
              <el-button :loading="geocoding" style="align-self:flex-end" @click="handleGeocode">查询坐标</el-button>
            </div>
          </el-form-item>
          <el-form-item label="经纬度" class="customer-home__form-span-2">
            <div style="display:flex;gap:8px;width:100%">
              <el-input-number v-model="form.receiverLng" :precision="6" :step="0.001" style="flex:1" placeholder="经度" />
              <el-input-number v-model="form.receiverLat" :precision="6" :step="0.001" style="flex:1" placeholder="纬度" />
            </div>
          </el-form-item>
          <el-form-item label="货物名称" prop="goodsName">
            <el-input v-model="form.goodsName" />
          </el-form-item>
          <el-form-item label="重量(kg)" prop="weight">
            <el-input-number v-model="form.weight" :min="0.1" :precision="2" class="customer-home__number" />
          </el-form-item>
          <el-form-item label="体积(m3)">
            <el-input-number v-model="form.volume" :min="0" :precision="3" class="customer-home__number" />
          </el-form-item>
          <el-form-item label="备注" class="customer-home__form-span-2">
            <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="200" show-word-limit />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          data-testid="customer-order-submit-button"
          @click="handleSubmit"
        >
          {{ editingId ? '保存修改' : '提交审核' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="detailVisible"
      title="订单详情"
      width="760px"
      data-testid="customer-order-detail-dialog"
    >
      <div v-loading="detailLoading" class="customer-home__detail">
        <template v-if="detailOrder">
          <el-alert
            v-if="isPendingReview(detailOrder)"
            title="当前订单仍处于待审核阶段，可继续编辑或取消。"
            type="warning"
            :closable="false"
            show-icon
            class="customer-home__detail-alert"
          />
          <el-alert
            v-else
            title="订单已进入审核后流程，客户侧仅保留查看权限。"
            type="info"
            :closable="false"
            show-icon
            class="customer-home__detail-alert"
          />

          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单号">{{ detailOrder.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusType(detailOrder.status)" size="small">
                {{ statusLabel(detailOrder.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="寄件人">{{ detailOrder.senderName || '--' }}</el-descriptions-item>
            <el-descriptions-item label="寄件电话">{{ detailOrder.senderPhone || '--' }}</el-descriptions-item>
            <el-descriptions-item label="寄件地址" :span="2">
              {{ detailOrder.senderAddress || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="收货人">{{ detailOrder.receiverName || '--' }}</el-descriptions-item>
            <el-descriptions-item label="收货电话">{{ detailOrder.receiverPhone || '--' }}</el-descriptions-item>
            <el-descriptions-item label="收货地址" :span="2">
              {{ detailOrder.receiverAddress || '--' }}
            </el-descriptions-item>
            <el-descriptions-item label="货物名称">{{ detailOrder.goodsName || '--' }}</el-descriptions-item>
            <el-descriptions-item label="重量/体积">
              {{ detailOrder.weight }} kg / {{ detailOrder.volume || 0 }} m3
            </el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">{{ detailOrder.remark || '--' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatTime(detailOrder.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatTime(detailOrder.updatedAt) }}</el-descriptions-item>
          </el-descriptions>

          <OrderChatPanel
            v-if="detailOrder"
            :order-id="detailOrder.id"
            class="customer-home__chat"
            panel-title="订单沟通"
          />
        </template>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="detailOrder && isPendingReview(detailOrder)"
          type="warning"
          data-testid="customer-order-detail-edit-button"
          @click="openEdit(detailOrder)"
        >
          编辑订单
        </el-button>
        <el-button
          v-if="detailOrder && isPendingReview(detailOrder)"
          type="danger"
          plain
          data-testid="customer-order-detail-cancel-button"
          @click="handleCancel(detailOrder)"
        >
          取消订单
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import OrderChatPanel from '@/components/OrderChatPanel.vue'
import { orderApi, type Order } from '@/api/order'
import { useAuthStore } from '@/stores/auth'
import { useGeocode } from '@/composables/useGeocode'

const { geocode, geocoding } = useGeocode()

const authStore = useAuthStore()

const statusOptions = [
  { value: 'pending_review', label: '待审核' },
  { value: 'pending', label: '待调度' },
  { value: 'dispatched', label: '已调度' },
  { value: 'in_progress', label: '配送中' },
  { value: 'completed', label: '已完成' },
  { value: 'cancelled', label: '已取消' },
  { value: 'exception', label: '异常' },
]

const displayName = computed(() => authStore.userInfo.realName || authStore.userInfo.username || '客户')
const currentStatusLabel = computed(() => {
  if (!query.status) {
    return '全部状态'
  }
  return statusLabel(query.status)
})
const emptyDescription = computed(() => {
  if (query.status || query.keyword) {
    return '当前筛选条件下暂无订单'
  }
  return '您还没有创建订单'
})

const loading = ref(false)
const tableData = ref<Order[]>([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  status: '',
  keyword: '',
})

const formVisible = ref(false)
const submitting = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailOrder = ref<Order | null>(null)

const form = reactive({
  senderName: '',
  senderPhone: '',
  senderAddress: '',
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  receiverLng: 0,
  receiverLat: 0,
  goodsName: '',
  weight: 1,
  volume: 0,
  remark: '',
})

const formRules: FormRules = {
  senderName: [{ required: true, message: '请输入寄件人姓名', trigger: 'blur' }],
  senderPhone: [{ required: true, message: '请输入寄件电话', trigger: 'blur' }],
  senderAddress: [{ required: true, message: '请输入寄件地址', trigger: 'blur' }],
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [{ required: true, message: '请输入收货电话', trigger: 'blur' }],
  receiverAddress: [{ required: true, message: '请输入收货地址', trigger: 'blur' }],
  receiverLng: [{ required: true, type: 'number' as const, min: 0.000001, message: '请输入有效收货经度', trigger: 'blur' }],
  receiverLat: [{ required: true, type: 'number' as const, min: 0.000001, message: '请输入有效收货纬度', trigger: 'blur' }],
  goodsName: [{ required: true, message: '请输入货物名称', trigger: 'blur' }],
  weight: [{ required: true, message: '请输入货物重量', trigger: 'change' }],
}

function statusLabel(status?: string) {
  return statusOptions.find(option => option.value === status)?.label ?? status ?? '--'
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

function isPendingReview(order?: Pick<Order, 'status'> | null) {
  return order?.status === 'pending_review'
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').substring(0, 16) : '--'
}

function applyFormDefaults() {
  Object.assign(form, {
    senderName: displayName.value,
    senderPhone: '',
    senderAddress: '',
    receiverName: '',
    receiverPhone: '',
    receiverAddress: '',
    receiverLng: 0,
    receiverLat: 0,
    goodsName: '',
    weight: 1,
    volume: 0,
    remark: '',
  })
}

function fillForm(order: Order) {
  Object.assign(form, {
    senderName: order.senderName || displayName.value,
    senderPhone: order.senderPhone || '',
    senderAddress: order.senderAddress || '',
    receiverName: order.receiverName || '',
    receiverPhone: order.receiverPhone || '',
    receiverAddress: order.receiverAddress || '',
    receiverLng: order.receiverLng ?? 0,
    receiverLat: order.receiverLat ?? 0,
    goodsName: order.goodsName || '',
    weight: order.weight || 1,
    volume: order.volume || 0,
    remark: order.remark || '',
  })
}

async function loadData() {
  loading.value = true
  try {
    const res = await orderApi.listMine({
      page: query.page,
      size: query.size,
      status: query.status || undefined,
      keyword: query.keyword || undefined,
    })
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
  query.page = 1
  query.size = 10
  query.status = ''
  query.keyword = ''
  loadData()
}

async function fetchOrder(id: number) {
  const res = await orderApi.getById(id)
  return res.data
}

async function openDetail(order: Order) {
  detailVisible.value = true
  detailLoading.value = true
  detailOrder.value = null
  try {
    detailOrder.value = await fetchOrder(order.id)
  } finally {
    detailLoading.value = false
  }
}

function openCreate() {
  editingId.value = null
  applyFormDefaults()
  formVisible.value = true
}

async function openEdit(order: Order) {
  if (!isPendingReview(order)) {
    ElMessage.warning('仅待审核订单支持编辑')
    return
  }

  const latestOrder = await fetchOrder(order.id)
  if (!isPendingReview(latestOrder)) {
    await loadData()
    detailOrder.value = latestOrder
    ElMessage.warning('订单状态已更新，当前不可编辑')
    return
  }

  editingId.value = latestOrder.id
  fillForm(latestOrder)
  detailVisible.value = false
  formVisible.value = true
}

function handleFormClosed() {
  editingId.value = null
  formRef.value?.resetFields()
  applyFormDefaults()
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
  const currentForm = formRef.value
  if (!currentForm) {
    return
  }

  await currentForm.validate()

  submitting.value = true
  const currentEditingId = editingId.value

  try {
    const payload = {
      senderName: form.senderName,
      senderPhone: form.senderPhone,
      senderAddress: form.senderAddress,
      senderLng: 0,
      senderLat: 0,
      receiverName: form.receiverName,
      receiverPhone: form.receiverPhone,
      receiverAddress: form.receiverAddress,
      receiverLng: form.receiverLng,
      receiverLat: form.receiverLat,
      goodsName: form.goodsName,
      weight: form.weight,
      volume: form.volume,
      remark: form.remark,
    }

    if (currentEditingId) {
      await orderApi.update(currentEditingId, payload)
      ElMessage.success('订单已更新，仍等待审核')
    } else {
      await orderApi.create(payload)
      ElMessage.success('订单已创建，等待审核')
    }

    formVisible.value = false
    await loadData()

    if (currentEditingId && detailOrder.value?.id === currentEditingId) {
      detailOrder.value = await fetchOrder(currentEditingId)
    }
  } finally {
    submitting.value = false
  }
}

async function handleCancel(order: Order) {
  if (!isPendingReview(order)) {
    ElMessage.warning('仅待审核订单支持取消')
    return
  }

  try {
    await ElMessageBox.confirm(
      '取消后订单将结束审核流，且不会进入调度。是否继续？',
      '取消订单',
      { type: 'warning' },
    )
  } catch {
    return
  }

  await orderApi.cancelPendingReview(order.id)
  ElMessage.success('订单已取消')
  await loadData()

  if (detailOrder.value?.id === order.id) {
    detailOrder.value = await fetchOrder(order.id)
  }
}

applyFormDefaults()
onMounted(loadData)
</script>

<style scoped>
.customer-home {
  display: grid;
  gap: var(--app-space-4);
}

.customer-home__card {
  box-shadow: var(--app-shadow-card);
}

.customer-home__hero {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--app-primary-soft) 56%, white)),
    linear-gradient(120deg, color-mix(in srgb, var(--app-warning) 10%, transparent), transparent 45%);
}

.customer-home__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--app-space-4);
}

.customer-home__eyebrow {
  color: var(--app-text-muted);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.customer-home__title-row {
  display: flex;
  align-items: center;
  gap: var(--app-space-3);
  margin-top: var(--app-space-3);
  flex-wrap: wrap;
}

.customer-home__title,
.customer-home__section-title {
  color: var(--app-text-strong);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.customer-home__section-title {
  font-size: 16px;
}

.customer-home__hero-content {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(240px, 1fr);
  gap: var(--app-space-5);
  align-items: start;
}

.customer-home__description {
  color: var(--app-text-secondary);
  line-height: 1.8;
  margin-bottom: var(--app-space-4);
}

.customer-home__metrics {
  display: grid;
  gap: var(--app-space-3);
}

.customer-home__metric {
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid color-mix(in srgb, var(--app-border) 90%, white);
  border-radius: var(--app-radius-md);
  padding: var(--app-space-4);
  box-shadow: var(--app-shadow-soft);
}

.customer-home__metric-label {
  display: block;
  color: var(--app-text-muted);
  font-size: 12px;
  margin-bottom: var(--app-space-3);
}

.customer-home__metric-value {
  color: var(--app-primary-dark);
  font-size: 24px;
  font-weight: 700;
}

.customer-home__metric-value--small {
  font-size: 18px;
}

.customer-home__toolbar {
  align-items: end;
}

.customer-home__field--sm {
  width: 140px;
}

.customer-home__field--md {
  width: 240px;
}

.customer-home__action-hint {
  color: var(--app-text-muted);
  font-size: 12px;
}

.customer-home__pagination {
  margin-top: var(--app-space-4);
  justify-content: flex-end;
}

.customer-home__form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: var(--app-space-4);
}

.customer-home__form-span-2 {
  grid-column: 1 / -1;
}

.customer-home__number {
  width: 100%;
}

.customer-home__detail {
  min-height: 220px;
}

.customer-home__detail-alert {
  margin-bottom: var(--app-space-4);
}

.customer-home__chat {
  margin-top: var(--app-space-4);
}

.customer-home :deep(.el-alert) {
  align-items: flex-start;
}

@media (max-width: 960px) {
  .customer-home__hero-content,
  .customer-home__form-grid {
    grid-template-columns: 1fr;
  }

  .customer-home__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .customer-home__field--sm,
  .customer-home__field--md,
  .customer-home__number {
    width: 100%;
  }
}
</style>
