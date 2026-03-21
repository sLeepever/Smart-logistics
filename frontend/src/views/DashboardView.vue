<template>
  <el-row :gutter="20">
    <el-col :span="6">
      <el-card shadow="hover">
        <template #header>待调度订单</template>
        <div class="kpi">{{ stats.pending }}</div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover">
        <template #header>今日完成订单</template>
        <div class="kpi">{{ stats.completed }}</div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover">
        <template #header>配送中订单</template>
        <div class="kpi">{{ stats.inProgress }}</div>
      </el-card>
    </el-col>
    <el-col :span="6">
      <el-card shadow="hover">
        <template #header>已调度订单</template>
        <div class="kpi">{{ stats.dispatched }}</div>
      </el-card>
    </el-col>
  </el-row>

  <el-card style="margin-top: 20px">
    <template #header>欢迎使用智慧物流调度系统</template>
    <p>登录成功！当前角色：<el-tag>{{ roleLabel }}</el-tag></p>
    <p style="margin-top: 12px; color: #999; font-size: 13px">
      调度方案、实时跟踪、数据统计模块正在开发中...
    </p>
  </el-card>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { orderApi } from '@/api/order'

const authStore = useAuthStore()

const roleLabel = computed(() => {
  const map: Record<string, string> = {
    admin: '管理员',
    dispatcher: '调度员',
    driver: '司机',
  }
  return map[authStore.userInfo.role] || authStore.userInfo.role
})

const stats = ref({ pending: 0, completed: 0, inProgress: 0, dispatched: 0 })

onMounted(async () => {
  try {
    const [p, c, ip, d] = await Promise.all([
      orderApi.list({ page: 1, size: 1, status: 'pending' }),
      orderApi.list({ page: 1, size: 1, status: 'completed' }),
      orderApi.list({ page: 1, size: 1, status: 'in_progress' }),
      orderApi.list({ page: 1, size: 1, status: 'dispatched' }),
    ])
    stats.value = {
      pending: p.data.total,
      completed: c.data.total,
      inProgress: ip.data.total,
      dispatched: d.data.total,
    }
  } catch {
    // ignore
  }
})
</script>

<style scoped>
.kpi {
  font-size: 36px;
  font-weight: 700;
  color: #1a6bcc;
  text-align: center;
  padding: 12px 0;
}
</style>
