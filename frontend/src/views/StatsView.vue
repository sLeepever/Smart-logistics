<template>
  <el-row :gutter="16">
    <!-- 订单状态分布 -->
    <el-col :span="12">
      <el-card>
        <template #header>订单状态分布</template>
        <div ref="orderPieRef" style="height:300px" />
      </el-card>
    </el-col>

    <!-- 车辆状态分布 -->
    <el-col :span="12">
      <el-card>
        <template #header>车辆状态分布</template>
        <div ref="vehiclePieRef" style="height:300px" />
      </el-card>
    </el-col>

    <!-- 调度效率对比 -->
    <el-col :span="24" style="margin-top:16px">
      <el-card>
        <template #header>调度方案效率对比（优化前 vs 优化后总里程 km）</template>
        <div ref="dispatchBarRef" style="height:300px" />
      </el-card>
    </el-col>

    <!-- KPI 汇总 -->
    <el-col :span="24" style="margin-top:16px">
      <el-row :gutter="16">
        <el-col :span="6" v-for="kpi in kpiList" :key="kpi.label">
          <el-card shadow="hover" style="text-align:center">
            <template #header>{{ kpi.label }}</template>
            <div style="font-size:32px;font-weight:700;color:#1a6bcc">{{ kpi.value }}</div>
            <div style="font-size:12px;color:#909399;margin-top:4px">{{ kpi.unit }}</div>
          </el-card>
        </el-col>
      </el-row>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import { orderApi } from '@/api/order'
import { vehicleApi } from '@/api/vehicle'
import { dispatchApi } from '@/api/dispatch'

const orderPieRef   = ref<HTMLElement>()
const vehiclePieRef = ref<HTMLElement>()
const dispatchBarRef = ref<HTMLElement>()

const kpiList = ref([
  { label: '总订单数',     value: '--', unit: '单' },
  { label: '已完成订单',   value: '--', unit: '单' },
  { label: '调度方案总数', value: '--', unit: '个' },
  { label: '平均里程节省', value: '--', unit: '%'  },
])

async function loadOrderPie() {
  const statuses = ['pending', 'dispatched', 'in_progress', 'completed', 'cancelled', 'exception']
  const labels   = ['待调度', '已调度', '配送中', '已完成', '已取消', '异常']
  const results  = await Promise.all(
    statuses.map(s => orderApi.list({ page: 1, size: 1, status: s }))
  )
  const data = results.map((r, i) => ({ name: labels[i], value: r.data.total }))

  const chart = echarts.init(orderPieRef.value!)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data,
      label: { formatter: '{b}\n{c}单' },
      emphasis: { itemStyle: { shadowBlur: 10 } },
    }],
  })

  const total     = data.reduce((s, d) => s + d.value, 0)
  const completed = data.find(d => d.name === '已完成')?.value ?? 0
  kpiList.value[0].value = String(total)
  kpiList.value[1].value = String(completed)
}

async function loadVehiclePie() {
  const statuses = ['idle', 'on_route', 'maintenance']
  const labels   = ['空闲', '在途', '维修中']
  const colors   = ['#67c23a', '#e6a23c', '#f56c6c']
  const results  = await Promise.all(
    statuses.map(s => vehicleApi.list({ page: 1, size: 1, status: s }))
  )
  const data = results.map((r, i) => ({ name: labels[i], value: r.data.total, itemStyle: { color: colors[i] } }))

  const chart = echarts.init(vehiclePieRef.value!)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data,
      label: { formatter: '{b}\n{c}辆' },
    }],
  })
}

async function loadDispatchBar() {
  const res   = await dispatchApi.listPlans({ page: 1, size: 20 })
  const plans = res.data.records as any[]

  kpiList.value[2].value = String(res.data.total)

  if (plans.length === 0) return

  const savings = plans
    .filter(p => p.beforeTotalDistance && p.afterTotalDistance)
    .map(p => ((p.beforeTotalDistance - p.afterTotalDistance) / p.beforeTotalDistance * 100).toFixed(1))
  if (savings.length > 0) {
    const avg = (savings.reduce((s, v) => s + Number(v), 0) / savings.length).toFixed(1)
    kpiList.value[3].value = avg
  }

  const chart = echarts.init(dispatchBarRef.value!)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['优化前里程', '优化后里程'] },
    xAxis: {
      type: 'category',
      data: plans.map(p => p.planNo.slice(-6)),
      axisLabel: { rotate: 30 },
    },
    yAxis: { type: 'value', name: 'km' },
    series: [
      {
        name: '优化前里程',
        type: 'bar',
        data: plans.map(p => p.beforeTotalDistance ?? 0),
        itemStyle: { color: '#f56c6c' },
      },
      {
        name: '优化后里程',
        type: 'bar',
        data: plans.map(p => p.afterTotalDistance ?? 0),
        itemStyle: { color: '#67c23a' },
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
