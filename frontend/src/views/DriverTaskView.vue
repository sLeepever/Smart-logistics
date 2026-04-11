<template>
  <div class="driver-task-view app-page">
    <el-card class="task-shell app-console-panel" shadow="never">
      <template #header>
        <div class="page-header">
          <div>
            <p class="page-eyebrow">今日任务</p>
            <h2 class="page-title">我的配送任务</h2>
            <p class="page-subtitle">仅展示当前司机的邀约与已认领路线，邀约阶段不会展示完整经停信息。</p>
          </div>
          <el-button :loading="isRefreshing" @click="loadPageData">刷新任务</el-button>
        </div>
      </template>

      <div class="summary-grid">
        <section class="summary-card summary-card--offer">
          <span class="summary-card__label">待响应邀约</span>
          <strong class="summary-card__value">{{ offers.length }}</strong>
          <span class="summary-card__hint">可选择接受或拒绝</span>
        </section>
        <section class="summary-card summary-card--active">
          <span class="summary-card__label">已接受 / 配送中</span>
          <strong class="summary-card__value">{{ activeRoutes.length }}</strong>
          <span class="summary-card__hint">展示完整路线与进度</span>
        </section>
        <section class="summary-card summary-card--completed">
          <span class="summary-card__label">已完成任务</span>
          <strong class="summary-card__value">{{ completedRoutes.length }}</strong>
          <span class="summary-card__hint">保留历史履约记录</span>
        </section>
      </div>

      <section class="task-section">
        <header class="section-header">
          <div>
            <h3>待响应邀约</h3>
            <p>邀约阶段仅显示概要字段；接受后才可查看站点与配送明细。</p>
          </div>
          <el-tag type="warning" effect="light">{{ offers.length }} 个待处理</el-tag>
        </header>

        <div v-if="offers.length > 0" class="card-list">
          <article
            v-for="offer in offers"
            :key="offer.routeId"
            class="task-card task-card--offer"
            :data-testid="`offer-card-${offer.routeId}`"
          >
            <div class="task-card__top">
              <div>
                <div class="task-card__title-row">
                  <span class="task-card__title">路线 #{{ offer.routeId }}</span>
                  <el-tag :type="statusType(offer.candidateStatus || offer.routeStatus)" size="small">
                    {{ offerStatusLabel(offer) }}
                  </el-tag>
                </div>
                <p class="task-card__caption">系统已为当前司机推送可认领路线，接受后将显示车辆与详细站点。</p>
              </div>
              <span class="task-card__time">邀约时间：{{ formatTime(offer.offeredAt) }}</span>
            </div>

            <div class="metric-grid">
              <div class="metric-item">
                <span class="metric-item__label">预计里程</span>
                <strong class="metric-item__value">{{ formatDistance(offer.estimatedDistance) }}</strong>
              </div>
              <div class="metric-item">
                <span class="metric-item__label">预计耗时</span>
                <strong class="metric-item__value">{{ formatDuration(offer.estimatedDuration) }}</strong>
              </div>
              <div class="metric-item">
                <span class="metric-item__label">邀约顺位</span>
                <strong class="metric-item__value">{{ offer.displayOrder ?? '--' }}</strong>
              </div>
            </div>

            <div class="task-card__actions">
              <el-button text @click="toggleOfferDetail(offer.routeId)">
                {{ isOfferExpanded(offer.routeId) ? '收起概要' : '查看概要' }}
              </el-button>
              <div class="action-group">
                <el-button
                  :loading="isActing('reject', offer.routeId)"
                  :data-testid="`offer-reject-${offer.routeId}`"
                  @click="handleOfferAction('reject', offer.routeId)"
                >
                  拒绝
                </el-button>
                <el-button
                  type="primary"
                  :loading="isActing('accept', offer.routeId)"
                  :data-testid="`offer-accept-${offer.routeId}`"
                  @click="handleOfferAction('accept', offer.routeId)"
                >
                  接受任务
                </el-button>
              </div>
            </div>

            <div
              v-if="isOfferExpanded(offer.routeId)"
              class="detail-panel detail-panel--summary"
              v-loading="Boolean(detailLoadingMap[offer.routeId])"
            >
              <el-descriptions :column="2" border size="small">
                <el-descriptions-item label="路线编号">#{{ offer.routeId }}</el-descriptions-item>
                <el-descriptions-item label="状态">{{ offerStatusLabel(offer) }}</el-descriptions-item>
                <el-descriptions-item label="预计里程">{{ formatDistance(getOfferDetail(offer.routeId)?.estimatedDistance ?? offer.estimatedDistance) }}</el-descriptions-item>
                <el-descriptions-item label="预计耗时">{{ formatDuration(getOfferDetail(offer.routeId)?.estimatedDuration ?? offer.estimatedDuration) }}</el-descriptions-item>
                <el-descriptions-item label="计划编号">{{ getOfferDetail(offer.routeId)?.planId ?? offer.planId }}</el-descriptions-item>
                <el-descriptions-item label="车辆编号">{{ getOfferDetail(offer.routeId)?.vehicleId ?? offer.vehicleId }}</el-descriptions-item>
              </el-descriptions>
                <p class="detail-note">
                  当前为邀约阶段，界面仅显示概要信息；完整经停点、地址与坐标会在接受任务并确认后展示。
                </p>
            </div>
          </article>
        </div>

        <el-empty v-else description="暂无待响应邀约" />
      </section>

      <section v-for="group in routeGroups" :key="group.key" class="task-section">
        <header class="section-header">
          <div>
            <h3>{{ group.title }}</h3>
            <p>{{ group.description }}</p>
          </div>
          <el-tag :type="group.tagType" effect="light">{{ group.items.length }} 条路线</el-tag>
        </header>

        <div v-if="group.items.length > 0" class="card-list">
          <article
            v-for="item in group.items"
            :key="item.route.id"
            class="task-card"
            :class="group.cardClass"
          >
            <div class="task-card__top">
              <div>
                <div class="task-card__title-row">
                  <span class="task-card__title">路线 #{{ item.route.id }}</span>
                  <el-tag :type="statusType(item.route.status)" size="small">
                    {{ statusLabel(item.route.status) }}
                  </el-tag>
                </div>
                <p class="task-card__caption">{{ routeCaption(item.route.status) }}</p>
              </div>
              <span class="task-card__time">{{ routeTimeLabel(item) }}</span>
            </div>

            <div class="metric-grid">
              <div class="metric-item">
                <span class="metric-item__label">预计里程</span>
                <strong class="metric-item__value">{{ formatDistance(item.route.estimatedDistance) }}</strong>
              </div>
              <div class="metric-item">
                <span class="metric-item__label">预计耗时</span>
                <strong class="metric-item__value">{{ formatDuration(item.route.estimatedDuration) }}</strong>
              </div>
              <div class="metric-item">
                <span class="metric-item__label">当前进度</span>
                <strong class="metric-item__value">{{ progressLabel(item) }}</strong>
              </div>
            </div>

            <div class="task-card__actions task-card__actions--single">
              <el-button text @click="toggleRouteDetail(item.route.id)">
                {{ isRouteExpanded(item.route.id) ? '收起详情' : '查看详情' }}
              </el-button>
            </div>

            <div
              v-if="isRouteExpanded(item.route.id)"
              class="detail-panel"
              :data-testid="`accepted-route-detail-${item.route.id}`"
              v-loading="Boolean(detailLoadingMap[item.route.id])"
            >
              <template v-if="canShowFullRouteDetail(item.route.id)">
                <el-descriptions :column="2" border size="small">
                  <el-descriptions-item label="路线状态">{{ statusLabel(item.route.status) }}</el-descriptions-item>
                  <el-descriptions-item label="计划编号">{{ getRouteDetail(item.route.id)?.planId ?? item.route.planId }}</el-descriptions-item>
                  <el-descriptions-item label="车辆编号">{{ getRouteDetail(item.route.id)?.vehicleId ?? item.route.vehicleId }}</el-descriptions-item>
                  <el-descriptions-item label="司机编号">{{ getRouteDetail(item.route.id)?.driverId ?? item.route.driverId }}</el-descriptions-item>
                </el-descriptions>

                <div class="detail-progress">
                  <span>站点进度</span>
                  <strong>{{ progressLabelFromStops(routeStops(item.route.id)) }}</strong>
                </div>

                <el-steps :active="activeStep(routeStops(item.route.id))" direction="vertical">
                  <el-step
                    v-for="stop in routeStops(item.route.id)"
                    :key="stop.id"
                    :title="`第${stop.stopSeq}站 - ${stop.stopType === 'pickup' ? '取货' : '送货'}`"
                    :status="stop.arrivedAt ? 'finish' : 'wait'"
                  >
                    <template #description>
                      <div class="stop-address">{{ stop.address }}</div>
                      <div v-if="stop.arrivedAt" class="stop-arrived-at">
                        已到达：{{ formatTime(stop.arrivedAt) }}
                      </div>
                    </template>
                  </el-step>
                </el-steps>

                <section v-if="deliveryOrderStops(item.route.id).length > 0" class="route-chat-section">
                  <div class="route-chat-section__header">
                    <div>
                      <h4>订单沟通</h4>
                        <p>以下仅展示当前已接受路线中的送货订单沟通。</p>
                    </div>
                    <el-tag type="primary" effect="light">
                      {{ deliveryOrderStops(item.route.id).length }} 个送货订单
                    </el-tag>
                  </div>

                  <div class="route-chat-list">
                    <article
                      v-for="stop in deliveryOrderStops(item.route.id)"
                      :key="`${item.route.id}-${stop.orderId}`"
                      class="route-chat-card"
                    >
                      <div class="route-chat-card__meta">
                        <div>
                          <strong>订单 #{{ stop.orderId }}</strong>
                          <p>{{ stop.address }}</p>
                        </div>
                        <el-tag size="small" effect="plain">第 {{ stop.stopSeq }} 站</el-tag>
                      </div>

                      <OrderChatPanel
                        :order-id="stop.orderId"
                        :panel-title="`订单 #${stop.orderId} 沟通`"
                        compact
                      />
                    </article>
                  </div>
                </section>
              </template>

              <el-empty v-else description="当前详情仍处于受限状态，请稍后刷新重试" :image-size="56" />
            </div>
          </article>
        </div>

        <el-empty v-else :description="group.emptyText" />
      </section>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import OrderChatPanel from '@/components/OrderChatPanel.vue'
import {
  dispatchApi,
  type DriverRouteOffer,
  type DriverRouteStop,
  type DriverRouteTask,
  type RouteDetail,
} from '@/api/dispatch'
import { useDriverOffers } from '@/composables/useDriverOffers'

const routeTasks = ref<DriverRouteTask[]>([])
const routeLoading = ref(false)

const expandedOfferIds = ref<number[]>([])
const expandedRouteIds = ref<number[]>([])

const detailMap = reactive<Record<number, RouteDetail | undefined>>({})
const detailLoadingMap = reactive<Record<number, boolean | undefined>>({})

const actionState = reactive({
  type: '' as 'accept' | 'reject' | '',
  routeId: 0,
})

const {
  offers,
  loading: offersLoading,
  refresh: refreshOffers,
  accept: acceptOffer,
  reject: rejectOffer,
} = useDriverOffers()

const activeRoutes = computed(() =>
  routeTasks.value.filter(item => ['accepted', 'in_progress'].includes(item.route.status)),
)

const completedRoutes = computed(() =>
  routeTasks.value.filter(item => item.route.status === 'completed'),
)

const routeGroups = computed(() => [
  {
    key: 'active',
    title: '已接受 / 配送中的任务',
    description: '接受成功后展示完整站点、当前进度与配送路线明细。',
    emptyText: '暂无进行中的路线任务',
    tagType: 'primary' as const,
    cardClass: 'task-card--active',
    items: activeRoutes.value,
  },
  {
    key: 'completed',
    title: '已完成任务',
    description: '保留已完成路线的履约详情，便于司机回看停靠记录。',
    emptyText: '暂无已完成任务',
    tagType: 'success' as const,
    cardClass: 'task-card--completed',
    items: completedRoutes.value,
  },
])

const isRefreshing = computed(() => routeLoading.value || offersLoading.value)

onMounted(() => {
  loadPageData()
})

async function loadPageData() {
  await Promise.all([loadRoutes(), refreshOffers()])
}

async function loadRoutes() {
  routeLoading.value = true
  try {
    const res = await dispatchApi.getDriverRoutes()
    routeTasks.value = res.data.filter(item => ['accepted', 'in_progress', 'completed'].includes(item.route.status))
  } catch {
    ElMessage.error('加载司机任务失败，请稍后重试')
  } finally {
    routeLoading.value = false
  }
}

function statusLabel(status?: string) {
  const map: Record<string, string> = {
    assigned: '待接受',
    offered: '待响应',
    accepted: '已接受',
    rejected: '已拒绝',
    offer_exhausted: '待重新分配',
    in_progress: '配送中',
    completed: '已完成',
  }
  return map[status ?? ''] || status || '--'
}

function statusType(status?: string) {
  const map: Record<string, '' | 'success' | 'warning' | 'info' | 'danger' | 'primary'> = {
    assigned: 'info',
    offered: 'warning',
    accepted: 'primary',
    rejected: 'danger',
    offer_exhausted: 'info',
    in_progress: 'warning',
    completed: 'success',
  }
  return map[status ?? ''] ?? 'info'
}

function offerStatusLabel(offer: DriverRouteOffer) {
  return statusLabel(offer.candidateStatus || offer.routeStatus)
}

function routeCaption(status: string) {
  if (status === 'completed') {
    return '该路线已完成配送，可回看停靠顺序与履约记录。'
  }
  if (status === 'in_progress') {
    return '路线执行中，完整经停点与到达记录已对当前司机可见。'
  }
  return '已接受路线，当前司机可查看完整经停点并按顺序执行。'
}

function routeTimeLabel(item: DriverRouteTask) {
  if (item.route.status === 'completed') {
    return `完成时间：${formatTime(item.route.completedAt)}`
  }
  if (item.route.startedAt) {
    return `开始时间：${formatTime(item.route.startedAt)}`
  }
  return `更新时间：${formatTime(item.route.updatedAt)}`
}

function formatTime(value?: string | null) {
  return value ? value.replace('T', ' ').substring(0, 16) : '--'
}

function formatDistance(value?: number | string | null) {
  if (value === null || value === undefined || value === '') return '--'
  const numeric = Number(value)
  return Number.isFinite(numeric) ? `${numeric.toFixed(1)} km` : `${value} km`
}

function formatDuration(value?: number | null) {
  if (value === null || value === undefined) return '--'
  return `${value} 分钟`
}

function progressLabel(item: DriverRouteTask) {
  return progressLabelFromStops(item.stops)
}

function progressLabelFromStops(stops: DriverRouteStop[]) {
  if (stops.length === 0) return '--'
  return `${activeStep(stops)}/${stops.length} 站`
}

function activeStep(stops: DriverRouteStop[]) {
  return stops.filter(stop => stop.arrivedAt).length
}

function isOfferExpanded(routeId: number) {
  return expandedOfferIds.value.includes(routeId)
}

function isRouteExpanded(routeId: number) {
  return expandedRouteIds.value.includes(routeId)
}

async function toggleOfferDetail(routeId: number) {
  if (isOfferExpanded(routeId)) {
    expandedOfferIds.value = expandedOfferIds.value.filter(id => id !== routeId)
    return
  }
  expandedOfferIds.value = [...expandedOfferIds.value, routeId]
  await ensureRouteDetail(routeId)
}

async function toggleRouteDetail(routeId: number) {
  if (isRouteExpanded(routeId)) {
    expandedRouteIds.value = expandedRouteIds.value.filter(id => id !== routeId)
    return
  }
  expandedRouteIds.value = [...expandedRouteIds.value, routeId]
  await ensureRouteDetail(routeId)
}

async function ensureRouteDetail(routeId: number, force = false) {
  if (!force && (detailMap[routeId] || detailLoadingMap[routeId])) return

  detailLoadingMap[routeId] = true
  try {
    const res = await dispatchApi.getRouteDetail(routeId)
    detailMap[routeId] = res.data
  } catch {
    ElMessage.error(`路线 #${routeId} 详情加载失败`)
  } finally {
    detailLoadingMap[routeId] = false
  }
}

function getRouteDetail(routeId: number) {
  return detailMap[routeId]
}

function getOfferDetail(routeId: number) {
  return detailMap[routeId]
}

function canShowFullRouteDetail(routeId: number) {
  return Boolean(detailMap[routeId]?.detailsVisible)
}

function routeStops(routeId: number) {
  return detailMap[routeId]?.stops ?? []
}

function deliveryOrderStops(routeId: number) {
  const deliveryStops = routeStops(routeId).filter(stop => stop.stopType === 'delivery' && stop.orderId > 0)
  const uniqueStops = new Map<number, DriverRouteStop>()

  deliveryStops.forEach((stop) => {
    if (!uniqueStops.has(stop.orderId)) {
      uniqueStops.set(stop.orderId, stop)
    }
  })

  return [...uniqueStops.values()]
}

function isActing(type: 'accept' | 'reject', routeId: number) {
  return actionState.type === type && actionState.routeId === routeId
}

async function handleOfferAction(type: 'accept' | 'reject', routeId: number) {
  actionState.type = type
  actionState.routeId = routeId

  try {
    if (type === 'accept') {
      await acceptOffer(routeId)
      ElMessage.success('已接受路线邀约')
      expandedOfferIds.value = expandedOfferIds.value.filter(id => id !== routeId)
      delete detailMap[routeId]
      delete detailLoadingMap[routeId]
      await loadRoutes()
      await ensureRouteDetail(routeId, true)
      expandedRouteIds.value = expandedRouteIds.value.includes(routeId)
        ? expandedRouteIds.value
        : [routeId, ...expandedRouteIds.value]
      return
    }

    await rejectOffer(routeId)
    ElMessage.success('已拒绝路线邀约')
    expandedOfferIds.value = expandedOfferIds.value.filter(id => id !== routeId)
    delete detailMap[routeId]
    await loadRoutes()
  } catch {
    ElMessage.error(type === 'accept' ? '接受路线失败，请稍后重试' : '拒绝路线失败，请稍后重试')
  } finally {
    actionState.type = ''
    actionState.routeId = 0
  }
}
</script>

<style scoped>
.driver-task-view {
  color: var(--app-text-primary);
}

.task-shell {
  overflow: hidden;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--app-space-4);
}

.page-eyebrow {
  color: var(--app-text-muted);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.16em;
  margin-bottom: var(--app-space-3);
  text-transform: uppercase;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--app-text-strong);
  line-height: 1.2;
}

.page-subtitle {
  color: var(--app-text-secondary);
  margin-top: var(--app-space-3);
  max-width: 720px;
}

.summary-grid {
  display: grid;
  gap: var(--app-space-4);
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: var(--app-space-6);
}

.summary-card {
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid color-mix(in srgb, var(--app-border) 90%, white);
  border-radius: var(--app-radius-lg);
  display: flex;
  flex-direction: column;
  gap: var(--app-space-3);
  min-height: 140px;
  padding: var(--app-space-5);
}

.summary-card--offer {
  background: linear-gradient(135deg, color-mix(in srgb, var(--app-warning-soft) 76%, white), rgba(255, 255, 255, 0.96));
}

.summary-card--active {
  background: linear-gradient(135deg, color-mix(in srgb, var(--app-primary-soft) 78%, white), rgba(255, 255, 255, 0.96));
}

.summary-card--completed {
  background: linear-gradient(135deg, color-mix(in srgb, var(--app-success) 16%, white), rgba(255, 255, 255, 0.96));
}

.summary-card__label {
  color: var(--app-text-secondary);
  font-size: 13px;
}

.summary-card__value {
  color: var(--app-text-strong);
  font-size: 34px;
  font-weight: 700;
  line-height: 1;
}

.summary-card__hint {
  color: var(--app-text-muted);
  font-size: 12px;
}

.task-section + .task-section {
  margin-top: var(--app-space-6);
}

.section-header {
  align-items: flex-start;
  display: flex;
  gap: var(--app-space-4);
  justify-content: space-between;
  margin-bottom: var(--app-space-4);
}

.section-header h3 {
  font-size: 18px;
  font-weight: 600;
}

.section-header p {
  color: var(--app-text-secondary);
  margin-top: var(--app-space-3);
}

.card-list {
  display: grid;
  gap: var(--app-space-4);
}

.task-card {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--app-surface-muted) 82%, white)),
    linear-gradient(135deg, color-mix(in srgb, var(--app-primary) 7%, transparent), transparent 58%);
  border: 1px solid color-mix(in srgb, var(--app-border) 90%, white);
  border-radius: var(--app-radius-lg);
  overflow: hidden;
  padding: var(--app-space-5);
}

.task-card--offer {
  border-color: var(--el-color-warning-light-5);
}

.task-card--active {
  border-color: var(--el-color-primary-light-5);
}

.task-card--completed {
  border-color: var(--el-color-success-light-5);
}

.task-card__top {
  align-items: flex-start;
  display: flex;
  gap: var(--app-space-4);
  justify-content: space-between;
}

.task-card__title-row {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: var(--app-space-3);
}

.task-card__title {
  font-size: 18px;
  font-weight: 600;
  color: var(--app-text-strong);
  letter-spacing: 0.04em;
}

.task-card__caption,
.task-card__time {
  color: var(--app-text-secondary);
  margin-top: var(--app-space-3);
}

.metric-grid {
  display: grid;
  gap: var(--app-space-4);
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: var(--app-space-5);
}

.metric-item {
  background: color-mix(in srgb, var(--app-primary-soft) 34%, white);
  border-radius: var(--app-radius-md);
  border: 1px solid color-mix(in srgb, var(--app-border) 84%, white);
  display: flex;
  flex-direction: column;
  gap: var(--app-space-3);
  min-height: 92px;
  padding: var(--app-space-4);
}

.metric-item__label {
  color: var(--app-text-secondary);
  font-size: 12px;
}

.metric-item__value {
  font-size: 22px;
  font-weight: 700;
  color: var(--app-text-strong);
  font-family: var(--app-font-mono);
  line-height: 1.1;
}

.task-card__actions {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin-top: var(--app-space-5);
}

.task-card__actions--single {
  justify-content: flex-end;
}

.action-group {
  display: flex;
  gap: var(--app-space-3);
}

.detail-panel {
  background: rgba(255, 255, 255, 0.9);
  border-radius: var(--app-radius-md);
  margin-top: var(--app-space-5);
  padding: var(--app-space-4);
}

.detail-panel--summary {
  border: 1px dashed var(--el-color-warning-light-5);
}

.detail-note {
  color: var(--app-text-secondary);
  line-height: 1.6;
  margin-top: var(--app-space-4);
}

.detail-progress {
  align-items: center;
  display: flex;
  justify-content: space-between;
  margin: var(--app-space-4) 0;
}

.detail-progress span {
  color: var(--app-text-secondary);
}

.detail-progress strong {
  color: color-mix(in srgb, var(--app-warning) 78%, white);
  font-size: 16px;
}

.stop-address {
  color: var(--app-text-primary);
}

.stop-arrived-at {
  color: var(--el-color-success);
  margin-top: var(--app-space-3);
}

.route-chat-section {
  border-top: 1px solid var(--app-border);
  margin-top: var(--app-space-5);
  padding-top: var(--app-space-5);
}

.route-chat-section__header {
  align-items: flex-start;
  display: flex;
  gap: var(--app-space-4);
  justify-content: space-between;
  margin-bottom: var(--app-space-4);
}

.route-chat-section__header h4 {
  font-size: 16px;
  font-weight: 600;
}

.route-chat-section__header p {
  color: var(--app-text-secondary);
  margin-top: 6px;
}

.route-chat-list {
  display: grid;
  gap: var(--app-space-4);
}

.route-chat-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid color-mix(in srgb, var(--app-border) 88%, white);
  border-radius: var(--app-radius-md);
  padding: var(--app-space-4);
}

.route-chat-card__meta {
  align-items: flex-start;
  display: flex;
  gap: var(--app-space-4);
  justify-content: space-between;
  margin-bottom: var(--app-space-4);
}

.route-chat-card__meta strong {
  color: color-mix(in srgb, var(--app-primary) 86%, white);
  font-size: 15px;
}

.route-chat-card__meta p {
  color: var(--app-text-secondary);
  margin-top: 6px;
}

@media (max-width: 960px) {
  .page-header,
  .section-header,
  .task-card__top,
  .task-card__actions,
  .route-chat-section__header,
  .route-chat-card__meta {
    align-items: stretch;
    flex-direction: column;
  }

  .summary-grid,
  .metric-grid {
    grid-template-columns: 1fr;
  }

  .task-card__actions--single {
    justify-content: flex-start;
  }

  .action-group {
    flex-wrap: wrap;
  }
}
</style>
