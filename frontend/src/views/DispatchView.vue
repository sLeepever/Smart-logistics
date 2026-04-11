<template>
  <div class="dispatch-view app-page">
    <el-card class="dispatch-view__panel app-console-panel" shadow="never">
      <template #header>
        <div class="dispatch-view__header">
          <div>
            <span class="dispatch-view__title">方案总览</span>
            <p class="dispatch-view__subtitle">保留原有生成、确认与详情逻辑，只让方案效率和路线结构更容易浏览。</p>
          </div>
          <el-button type="primary" :loading="generating" @click="handleGenerate">
            生成调度方案
          </el-button>
        </div>
      </template>

      <el-table :data="plans" v-loading="loading" border stripe class="dispatch-view__table" @row-click="openDetail">
        <el-table-column prop="planNo" label="方案编号" width="160" />
        <el-table-column prop="totalOrders" label="订单数" width="80" align="center" />
        <el-table-column prop="totalRoutes" label="路线数" width="80" align="center" />
        <el-table-column label="优化前总距离(km)" width="150" align="right">
          <template #default="{ row }">{{ row.beforeTotalDistance?.toFixed(1) }}</template>
        </el-table-column>
        <el-table-column label="优化后总距离(km)" width="150" align="right">
          <template #default="{ row }">
            <span class="dispatch-view__distance-value dispatch-view__distance-value--after">{{ row.afterTotalDistance?.toFixed(1) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="里程节省" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.beforeTotalDistance" class="dispatch-view__saving-rate">
              {{ savingRate(row) }}%
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="planStatusType(row.status)" size="small">{{ planStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 'draft'"
              link
              type="success"
              size="small"
              @click.stop="handleConfirm(row.id)"
            >确认</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="dispatch-view__pagination"
        v-model:current-page="page"
        :total="total"
        layout="total, prev, pager, next"
        @change="loadPlans"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="调度方案详情" width="900px" class="dispatch-view__dialog">
      <div v-if="detail" class="dispatch-view__detail">
        <el-descriptions :column="4" border size="small" class="dispatch-view__summary">
          <el-descriptions-item label="方案编号">{{ detail.plan?.planNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="planStatusType(detail.plan?.status)" size="small">{{ planStatusLabel(detail.plan?.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订单数">{{ detail.plan?.totalOrders }}</el-descriptions-item>
          <el-descriptions-item label="路线数">{{ detail.plan?.totalRoutes }}</el-descriptions-item>
          <el-descriptions-item label="优化前距离">{{ detail.plan?.beforeTotalDistance?.toFixed(1) }} km</el-descriptions-item>
          <el-descriptions-item label="优化后距离">
            <span class="dispatch-view__distance-value dispatch-view__distance-value--after">{{ detail.plan?.afterTotalDistance?.toFixed(1) }} km</span>
          </el-descriptions-item>
          <el-descriptions-item label="节省里程">
            <span class="dispatch-view__saving-rate">{{ savingKm(detail.plan) }} km ({{ savingRate(detail.plan) }}%)</span>
          </el-descriptions-item>
          <el-descriptions-item label="使用车辆">{{ detail.plan?.afterVehicleCount }} / {{ detail.plan?.beforeVehicleCount }} 辆</el-descriptions-item>
        </el-descriptions>

        <el-collapse>
          <el-collapse-item
            v-for="(rv, idx) in detail.routes"
            :key="rv.route.id"
            :title="`路线 ${Number(idx) + 1}  —  车辆ID: ${rv.route.vehicleId ?? '待接受后锁定'}  |  预估 ${rv.route.estimatedDistance} km  |  ${rv.route.estimatedDuration} 分钟`"
            :name="idx"
          >
            <el-table :data="rv.stops" border size="small">
              <el-table-column prop="stopSeq" label="序号" width="60" align="center" />
              <el-table-column label="类型" width="70" align="center">
                <template #default="{ row }">
                  <el-tag :type="row.stopType === 'pickup' ? 'info' : 'success'" size="small">
                    {{ row.stopType === 'pickup' ? '取货' : '送货' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="orderId" label="订单ID" width="80" align="center" />
              <el-table-column prop="address" label="地址" show-overflow-tooltip />
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detail?.plan?.status === 'draft'" type="success" @click="handleConfirm(detail.plan.id)">确认方案</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { dispatchApi, type DispatchPlan, type DispatchPlanDetail } from '@/api/dispatch'

const planStatusOptions = [
  { value: 'draft', label: '草稿', type: '' as const },
  { value: 'confirmed', label: '已确认', type: 'warning' as const },
  { value: 'executing', label: '执行中', type: 'success' as const },
  { value: 'completed', label: '已完成', type: 'info' as const },
]
const planStatusLabel = (s?: string) => planStatusOptions.find(o => o.value === s)?.label ?? s ?? ''
const planStatusType = (s?: string) => planStatusOptions.find(o => o.value === s)?.type ?? ''

function savingRate(plan?: DispatchPlan | null) {
  const beforeDistance = plan?.beforeTotalDistance ?? 0
  const afterDistance = plan?.afterTotalDistance ?? 0
  if (beforeDistance === 0) return '0.0'
  return (((beforeDistance - afterDistance) / beforeDistance) * 100).toFixed(1)
}
function savingKm(plan?: DispatchPlan | null) {
  const beforeDistance = plan?.beforeTotalDistance ?? 0
  const afterDistance = plan?.afterTotalDistance ?? 0
  return (beforeDistance - afterDistance).toFixed(1)
}
function formatTime(t: string) {
  return t ? t.replace('T', ' ').substring(0, 16) : ''
}

const loading = ref(false)
const plans = ref<DispatchPlan[]>([])
const total = ref(0)
const page = ref(1)

async function loadPlans() {
  loading.value = true
  try {
    const res = await dispatchApi.listPlans({ page: page.value, size: 10 })
    plans.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}
onMounted(loadPlans)

const generating = ref(false)
async function handleGenerate() {
  await ElMessageBox.confirm(
    '将对所有已通过审核的待调度订单运行 K-Means + 遗传算法生成调度方案，是否继续？',
    '生成调度方案',
    { type: 'warning' }
  )
  generating.value = true
  try {
    const res = await dispatchApi.generatePlan()
    ElMessage.success('方案生成成功')
    await loadPlans()
    openDetailData(res.data)
  } finally {
    generating.value = false
  }
}

const detailVisible = ref(false)
const detail = ref<DispatchPlanDetail | null>(null)

async function openDetail(row: DispatchPlan) {
  const res = await dispatchApi.getPlanDetail(row.id)
  openDetailData(res.data)
}
function openDetailData(data: DispatchPlanDetail) {
  detail.value = data
  detailVisible.value = true
}

async function handleConfirm(id: number) {
  await ElMessageBox.confirm(
    '确认方案后，订单状态将变更为「已调度」，车辆变更为「在途」，此操作不可撤销。',
    '确认调度方案',
    { type: 'warning' }
  )
  await dispatchApi.confirmPlan(id)
  ElMessage.success('方案已确认，订单已调度')
  detailVisible.value = false
  loadPlans()
}
</script>

<style scoped>
.dispatch-view__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--app-space-4);
}

.dispatch-view__title {
  display: block;
  font-size: 16px;
  font-weight: 700;
  color: var(--app-text-strong);
  letter-spacing: 0.04em;
}

.dispatch-view__subtitle {
  margin-top: 6px;
  color: var(--app-text-secondary);
  font-size: 13px;
}

.dispatch-view__table {
  width: 100%;
}

.dispatch-view__distance-value--after,
.dispatch-view__saving-rate {
  font-weight: 700;
}

.dispatch-view__distance-value--after {
  color: color-mix(in srgb, var(--app-success) 86%, white);
}

.dispatch-view__saving-rate {
  color: color-mix(in srgb, var(--app-warning) 88%, white);
}

.dispatch-view__pagination {
  margin-top: var(--app-space-4);
  justify-content: flex-end;
}

.dispatch-view__detail {
  display: grid;
  gap: var(--app-space-4);
}

.dispatch-view__summary {
  margin-bottom: 0;
}

.dispatch-view__detail :deep(.el-collapse) {
  background: transparent;
}

@media (max-width: 768px) {
  .dispatch-view__header {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
