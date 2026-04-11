<template>
  <div class="stats-view app-page">
    <el-row :gutter="16" class="stats-view__grid">
      <el-col :xs="24" :lg="12">
        <el-card class="stats-view__chart-card app-console-panel" shadow="never">
          <template #header>
            <div class="stats-view__card-header">
              <span class="stats-view__card-title">订单状态分布</span>
              <span class="stats-view__card-caption">Order Lifecycle</span>
            </div>
          </template>
          <div ref="orderPieRef" class="stats-view__chart" />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card class="stats-view__chart-card app-console-panel" shadow="never">
          <template #header>
            <div class="stats-view__card-header">
              <span class="stats-view__card-title">车辆状态分布</span>
              <span class="stats-view__card-caption">Fleet Readiness</span>
            </div>
          </template>
          <div ref="vehiclePieRef" class="stats-view__chart" />
        </el-card>
      </el-col>

      <el-col :xs="24">
        <el-card class="stats-view__chart-card app-console-panel" shadow="never">
          <template #header>
            <div class="stats-view__card-header">
              <span class="stats-view__card-title">调度方案效率对比（优化前 vs 优化后总里程 km）</span>
              <span class="stats-view__card-caption">Dispatch Efficiency</span>
            </div>
          </template>
          <div ref="dispatchBarRef" class="stats-view__chart stats-view__chart--wide" />
        </el-card>
      </el-col>

      <el-col :xs="24">
        <div class="stats-view__kpi-grid">
          <el-card v-for="kpi in kpiList" :key="kpi.label" class="stats-view__kpi-card app-console-panel" shadow="never">
            <span class="stats-view__kpi-label">{{ kpi.label }}</span>
            <div class="stats-view__kpi-value">{{ kpi.value }}</div>
            <div class="stats-view__kpi-unit">{{ kpi.unit }}</div>
          </el-card>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { orderApi } from '@/api/order'
import { vehicleApi } from '@/api/vehicle'
import { dispatchApi, type DispatchPlan } from '@/api/dispatch'

const orderPieRef = ref<HTMLElement>()
const vehiclePieRef = ref<HTMLElement>()
const dispatchBarRef = ref<HTMLElement>()

const kpiList = ref([
  { label: '总订单数', value: '--', unit: '单' },
  { label: '已完成订单', value: '--', unit: '单' },
  { label: '调度方案总数', value: '--', unit: '个' },
  { label: '平均里程节省', value: '--', unit: '%' },
])

function setKpiValue(index: number, value: string) {
  const item = kpiList.value[index]
  if (item) {
    item.value = value
  }
}

function getCssVar(name: string) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

function createChartTheme() {
  return {
    primary: getCssVar('--app-primary') || '#3c7ea6',
    primarySoft: getCssVar('--app-primary-soft') || '#d5e6f1',
    warning: getCssVar('--app-warning') || '#c98c2b',
    success: getCssVar('--app-success') || '#4d8b68',
    danger: getCssVar('--app-danger') || '#b75e54',
    text: getCssVar('--app-text-primary') || '#16202a',
    textSecondary: getCssVar('--app-text-secondary') || '#4d5a66',
    textMuted: getCssVar('--app-text-muted') || '#72808d',
    grid: getCssVar('--app-border') || '#b7c1cb',
    panel: getCssVar('--app-surface-muted') || '#e7edf2',
  }
}

async function loadOrderPie() {
  const theme = createChartTheme()
  const statuses = ['pending_review', 'pending', 'dispatched', 'in_progress', 'completed', 'cancelled', 'exception']
  const labels = ['待审核', '待调度', '已调度', '配送中', '已完成', '已取消', '异常']
  const colors = [theme.warning, theme.primary, '#567b95', theme.primarySoft, theme.success, theme.textMuted, theme.danger]
  const results = await Promise.all(statuses.map(s => orderApi.list({ page: 1, size: 1, status: s })))
  const data = results.map((r, i) => ({ name: labels[i], value: r.data.total, itemStyle: { color: colors[i] } }))

  const chart = echarts.init(orderPieRef.value!)
  chart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: {
      bottom: 0,
      textStyle: { color: theme.textSecondary },
    },
    series: [{
      type: 'pie',
      radius: ['42%', '70%'],
      data,
      label: { color: theme.text, formatter: '{b}\n{c}单' },
      itemStyle: { borderColor: theme.panel, borderWidth: 3 },
      emphasis: { itemStyle: { shadowBlur: 14, shadowColor: 'rgba(10, 16, 24, 0.2)' } },
    }],
  })

  const total = data.reduce((s, d) => s + d.value, 0)
  const completed = data.find(d => d.name === '已完成')?.value ?? 0
  setKpiValue(0, String(total))
  setKpiValue(1, String(completed))
}

async function loadVehiclePie() {
  const theme = createChartTheme()
  const statuses = ['idle', 'on_route', 'maintenance']
  const labels = ['空闲', '在途', '维修中']
  const colors = [theme.success, theme.warning, theme.danger]
  const results = await Promise.all(statuses.map(s => vehicleApi.list({ page: 1, size: 1, status: s })))
  const data = results.map((r, i) => ({ name: labels[i], value: r.data.total, itemStyle: { color: colors[i] } }))

  const chart = echarts.init(vehiclePieRef.value!)
  chart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: {
      bottom: 0,
      textStyle: { color: theme.textSecondary },
    },
    series: [{
      type: 'pie',
      radius: ['42%', '70%'],
      data,
      label: { color: theme.text, formatter: '{b}\n{c}辆' },
      itemStyle: { borderColor: theme.panel, borderWidth: 3 },
    }],
  })
}

async function loadDispatchBar() {
  const theme = createChartTheme()
  const res = await dispatchApi.listPlans({ page: 1, size: 20 })
  const plans: DispatchPlan[] = res.data.records

  setKpiValue(2, String(res.data.total))

  if (plans.length === 0) return

  const savings = plans
    .map((plan) => {
      const beforeDistance = plan.beforeTotalDistance ?? 0
      const afterDistance = plan.afterTotalDistance ?? 0
      if (beforeDistance === 0) {
        return null
      }
      return (((beforeDistance - afterDistance) / beforeDistance) * 100).toFixed(1)
    })
    .filter((value): value is string => value !== null)
  if (savings.length > 0) {
    const avg = (savings.reduce((s, v) => s + Number(v), 0) / savings.length).toFixed(1)
    setKpiValue(3, avg)
  }

  const chart = echarts.init(dispatchBarRef.value!)
  chart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis' },
    legend: {
      data: ['优化前里程', '优化后里程'],
      textStyle: { color: theme.textSecondary },
    },
    grid: { left: 48, right: 24, top: 40, bottom: 72 },
    xAxis: {
      type: 'category',
      data: plans.map(p => p.planNo.slice(-6)),
      axisLabel: { rotate: 30, color: theme.textSecondary },
      axisLine: { lineStyle: { color: theme.grid } },
    },
    yAxis: {
      type: 'value',
      name: 'km',
      nameTextStyle: { color: theme.textMuted },
      axisLabel: { color: theme.textSecondary },
      splitLine: { lineStyle: { color: theme.grid, type: 'dashed' } },
    },
    series: [
      {
        name: '优化前里程',
        type: 'bar',
        data: plans.map(p => p.beforeTotalDistance ?? 0),
        itemStyle: { color: theme.warning, borderRadius: [6, 6, 0, 0] },
      },
      {
        name: '优化后里程',
        type: 'bar',
        data: plans.map(p => p.afterTotalDistance ?? 0),
        itemStyle: { color: theme.primary, borderRadius: [6, 6, 0, 0] },
      },
    ],
  })
}

onMounted(() => {
  loadOrderPie()
  loadVehiclePie()
  loadDispatchBar()
})
</script>

<style scoped>
.stats-view__grid {
  margin: 0;
}

.stats-view__chart-card {
  height: 100%;
}

.stats-view__card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--app-space-4);
}

.stats-view__card-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--app-text-strong);
  letter-spacing: 0.04em;
}

.stats-view__card-caption,
.stats-view__kpi-label,
.stats-view__kpi-unit {
  color: var(--app-text-muted);
  font-size: 11px;
  font-weight: 700;
  font-family: var(--app-font-mono);
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.stats-view__chart {
  height: 320px;
}

.stats-view__chart--wide {
  height: 340px;
}

.stats-view__kpi-grid {
  display: grid;
  gap: var(--app-space-4);
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.stats-view__kpi-card {
  display: grid;
  gap: var(--app-space-3);
  min-height: 184px;
  align-content: start;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--app-primary-soft) 42%, white)),
    linear-gradient(135deg, color-mix(in srgb, var(--app-warning) 8%, transparent), transparent 58%);
}

.stats-view__kpi-value {
  color: var(--app-text-strong);
  font-size: 38px;
  font-weight: 700;
  letter-spacing: 0.08em;
  line-height: 1;
}

.stats-view__kpi-unit {
  font-size: 10px;
}

@media (max-width: 960px) {
  .stats-view__kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .stats-view__card-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .stats-view__kpi-grid {
    grid-template-columns: 1fr;
  }
}
</style>
