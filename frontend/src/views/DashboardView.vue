<template>
  <div class="dashboard-view app-page">
    <el-row :gutter="16" class="dashboard-view__kpi-grid">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="dashboard-view__kpi-card dashboard-view__kpi-card--warning app-console-panel" shadow="never">
          <span class="dashboard-view__kpi-label">待调度订单</span>
          <div class="dashboard-view__kpi-value">{{ stats.pending }}</div>
          <span class="dashboard-view__kpi-meta">待进入调度排程，需优先完成资源分配</span>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="dashboard-view__kpi-card app-console-panel" shadow="never">
          <span class="dashboard-view__kpi-label">今日完成订单</span>
          <div class="dashboard-view__kpi-value">{{ stats.completed }}</div>
          <span class="dashboard-view__kpi-meta">已闭环交付并完成履约记录</span>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="dashboard-view__kpi-card dashboard-view__kpi-card--active app-console-panel" shadow="never">
          <span class="dashboard-view__kpi-label">配送中订单</span>
          <div class="dashboard-view__kpi-value">{{ stats.inProgress }}</div>
          <span class="dashboard-view__kpi-meta">正在执行路线，需持续关注轨迹反馈</span>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="dashboard-view__kpi-card dashboard-view__kpi-card--info app-console-panel" shadow="never">
          <span class="dashboard-view__kpi-label">已调度订单</span>
          <div class="dashboard-view__kpi-value">{{ stats.dispatched }}</div>
          <span class="dashboard-view__kpi-meta">已进入路线分配，等待司机执行</span>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="dashboard-view__summary app-console-panel" shadow="never">
      <template #header>
        <div class="dashboard-view__summary-header">
          <div>
            <span class="app-section-label">今日提示</span>
            <h2 class="dashboard-view__summary-title">欢迎使用智慧物流调度系统</h2>
          </div>
          <el-tag effect="plain">{{ roleLabel }}</el-tag>
        </div>
      </template>

      <div class="dashboard-view__summary-body">
        <p>
          登录成功。当前界面已切换为 <strong>{{ roleLabel }}</strong>
          视角，可以继续进入订单、调度、实时追踪与统计模块处理日常任务。
        </p>
        <p class="dashboard-view__summary-note">调度方案、实时追踪和数据统计模块已经能支撑完整演示流程，后续仍可继续补充更多细节内容。</p>
      </div>
    </el-card>
  </div>
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
.dashboard-view__kpi-grid {
  margin: 0;
}

.dashboard-view__kpi-card {
  min-height: 196px;
  display: grid;
  gap: var(--app-space-3);
  align-content: start;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--app-primary-soft) 42%, white)),
    linear-gradient(135deg, color-mix(in srgb, var(--app-primary) 10%, transparent), transparent 56%);
}

.dashboard-view__kpi-card--warning {
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--app-warning-soft) 72%, white), rgba(255, 255, 255, 0.98)),
    linear-gradient(135deg, color-mix(in srgb, var(--app-warning) 14%, transparent), transparent 62%);
}

.dashboard-view__kpi-card--active {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--app-primary-soft) 58%, white)),
    linear-gradient(135deg, color-mix(in srgb, var(--app-primary) 14%, transparent), transparent 60%);
}

.dashboard-view__kpi-card--info {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--app-warning-soft) 34%, white)),
    linear-gradient(135deg, rgba(255, 255, 255, 0.48), transparent 60%);
}

.dashboard-view__kpi-label {
  color: var(--app-text-muted);
  font-size: 11px;
  font-weight: 700;
  font-family: var(--app-font-mono);
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.dashboard-view__kpi-value {
  color: var(--app-text-strong);
  font-size: 42px;
  font-weight: 700;
  letter-spacing: 0.08em;
  line-height: 1;
}

.dashboard-view__kpi-meta {
  color: var(--app-text-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.dashboard-view__summary-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--app-space-4);
}

.dashboard-view__summary-title {
  margin-top: 8px;
  color: var(--app-text-strong);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.dashboard-view__summary-body {
  display: grid;
  gap: var(--app-space-4);
  color: var(--app-text-secondary);
  line-height: 1.8;
}

.dashboard-view__summary-body strong {
  color: color-mix(in srgb, var(--app-warning) 80%, white);
}

.dashboard-view__summary-note {
  padding: var(--app-space-4);
  border: 1px dashed color-mix(in srgb, var(--app-border) 88%, white);
  border-radius: var(--app-radius-md);
  background: color-mix(in srgb, var(--app-primary-soft) 42%, white);
}

@media (max-width: 768px) {
  .dashboard-view__summary-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
