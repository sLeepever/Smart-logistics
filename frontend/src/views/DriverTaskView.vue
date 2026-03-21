<template>
  <div>
    <el-card>
      <template #header>
        <span style="font-weight:600">我的配送任务</span>
      </template>

      <el-empty v-if="tasks.length === 0" description="暂无配送任务" />

      <div v-for="item in tasks" :key="item.route.id" class="route-card">
        <div class="route-header">
          <div>
            <span class="route-title">路线 #{{ item.route.id }}</span>
            <el-tag :type="statusType(item.route.status)" size="small" style="margin-left:10px">
              {{ statusLabel(item.route.status) }}
            </el-tag>
          </div>
          <div style="font-size:13px;color:#909399">
            预计里程：{{ item.route.estimatedDistance ?? '--' }} km
          </div>
        </div>

        <el-steps :active="activeStep(item)" direction="vertical" style="margin-top:16px;padding-left:8px">
          <el-step
            v-for="stop in item.stops"
            :key="stop.id"
            :title="`第${stop.stopSeq}站 - ${stop.stopType === 'pickup' ? '取货' : '送货'}`"
            :description="stop.address"
            :status="stop.arrivedAt ? 'finish' : 'wait'"
          >
            <template #description>
              <div>{{ stop.address }}</div>
              <div v-if="stop.arrivedAt" style="color:#67c23a;font-size:12px;margin-top:2px">
                已到达：{{ stop.arrivedAt.replace('T', ' ').substring(0, 16) }}
              </div>
            </template>
          </el-step>
        </el-steps>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const tasks = ref<any[]>([])

onMounted(async () => {
  try {
    const res = await request.get<any, { data: any[] }>('/dispatch/driver/routes')
    tasks.value = res.data
  } catch {
    // ignore
  }
})

function statusLabel(s: string) {
  const map: Record<string, string> = {
    assigned: '待接受', accepted: '已接受', in_progress: '配送中', completed: '已完成',
  }
  return map[s] || s
}

function statusType(s: string) {
  const map: Record<string, string> = {
    assigned: 'info', accepted: 'warning', in_progress: 'primary', completed: 'success',
  }
  return map[s] || 'info'
}

function activeStep(item: any) {
  const arrived = item.stops.filter((s: any) => s.arrivedAt).length
  return arrived
}
</script>

<style scoped>
.route-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}
.route-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.route-title {
  font-weight: 600;
  font-size: 15px;
}
</style>
