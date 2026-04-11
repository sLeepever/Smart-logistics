<template>
  <el-container class="app-shell">
    <el-header class="app-header">
      <div class="app-header__brand">
        <div class="app-header__mark">SL</div>
        <div>
          <span class="logo">智慧物流调度系统</span>
          <p class="app-header__subtitle">清爽协同物流工作区</p>
        </div>
      </div>

      <div class="header-right">
        <div class="header-right__identity">
          <span class="header-right__label">当前用户</span>
          <strong>{{ authStore.userInfo.realName }}</strong>
          <span class="header-right__role">{{ roleLabel }}</span>
        </div>
        <el-button text type="primary" class="header-right__action" @click="handleLogout">
          退出登录
        </el-button>
      </div>
    </el-header>

    <el-container class="app-body">
      <el-aside width="220px" class="app-aside">
        <div class="app-aside__caption">快捷导航</div>
        <el-menu
          class="app-menu"
          :default-active="$route.path"
          router
          background-color="transparent"
          text-color="var(--app-text-strong)"
          active-text-color="var(--app-text-strong)"
        >
          <el-menu-item v-for="(item, index) in visibleMenuItems" :key="item.path" :index="item.path">
            <span class="app-menu__index">{{ String(index + 1).padStart(2, '0') }}</span>
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </el-menu>

        <div class="app-aside__footer">
          <span class="app-aside__footer-label">今日关注</span>
          <strong>订单、车辆与运输进度</strong>
        </div>
      </el-aside>

      <el-main class="app-main">
        <div class="app-main__frame">
          <div class="app-main__frame-label">业务工作区</div>
          <div class="app-main__viewport">
            <router-view />
          </div>
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, type Component } from 'vue'
import {
  Odometer,
  List,
  UserFilled,
  Van,
  Calendar,
  Location,
  DataAnalysis,
  HomeFilled,
} from '@element-plus/icons-vue'
import { roleLabels, useAuthStore, type AppRole } from '@/stores/auth'

const authStore = useAuthStore()

type MenuItem = {
  path: string
  label: string
  icon: Component
  roles: AppRole[]
}

const menuItems: MenuItem[] = [
  { path: '/dashboard', label: '运行概览', icon: Odometer, roles: ['admin', 'dispatcher'] },
  { path: '/orders', label: '订单管理', icon: List, roles: ['admin', 'dispatcher'] },
  { path: '/users', label: '用户管理', icon: UserFilled, roles: ['admin'] },
  { path: '/vehicles', label: '车辆管理', icon: Van, roles: ['admin', 'dispatcher'] },
  { path: '/dispatch', label: '调度方案', icon: Calendar, roles: ['admin', 'dispatcher'] },
  { path: '/tracking', label: '实时追踪', icon: Location, roles: ['admin', 'dispatcher'] },
  { path: '/stats', label: '数据统计', icon: DataAnalysis, roles: ['admin', 'dispatcher'] },
  { path: '/driver/tasks', label: '我的任务', icon: Van, roles: ['driver'] },
  { path: '/customer/home', label: '我的订单', icon: HomeFilled, roles: ['customer'] },
]

const currentRole = computed(() => authStore.userInfo.role as AppRole)

const roleLabel = computed(() => {
  return roleLabels[currentRole.value] || authStore.userInfo.role
})

const visibleMenuItems = computed(() => menuItems.filter(item => item.roles.includes(currentRole.value)))

function handleLogout() {
  authStore.logout()
}
</script>

<style scoped>
.app-header {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: var(--app-header-height);
  padding: 0 var(--app-space-6);
  border-bottom: 1px solid color-mix(in srgb, var(--app-border) 88%, white);
  background: color-mix(in srgb, var(--app-surface) 92%, white);
  color: var(--app-text-strong);
  box-shadow: var(--app-shadow-soft);
  z-index: 20;
}

.app-header::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at left center, color-mix(in srgb, var(--app-primary) 16%, transparent), transparent 34%),
    linear-gradient(90deg, color-mix(in srgb, var(--app-warning) 18%, transparent), transparent 24%);
  opacity: 0.75;
}

.app-header::after {
  content: '';
  position: absolute;
  inset: auto 0 0 0;
  height: 1px;
  background: linear-gradient(90deg, var(--app-primary), color-mix(in srgb, var(--app-warning) 62%, white), transparent 82%);
}

.app-header__brand {
  display: flex;
  align-items: center;
  gap: var(--app-space-4);
}

.app-header__mark {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border: 1px solid color-mix(in srgb, var(--app-border-strong) 92%, white);
  border-radius: var(--app-radius-lg);
  background: linear-gradient(145deg, color-mix(in srgb, var(--app-primary) 18%, white), color-mix(in srgb, var(--app-warning) 28%, white));
  color: var(--app-primary-dark);
  font-size: 18px;
  font-weight: 700;
  font-family: var(--app-font-mono);
  letter-spacing: 0.14em;
  box-shadow: var(--app-shadow-soft);
}

.logo {
  display: block;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.app-header__subtitle {
  margin-top: 4px;
  color: var(--app-text-secondary);
  font-size: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--app-space-4);
}

.header-right__identity {
  display: grid;
  justify-items: end;
  padding: 10px 14px;
  border: 1px solid color-mix(in srgb, var(--app-border) 90%, white);
  border-radius: var(--app-radius-lg);
  background: color-mix(in srgb, var(--app-surface) 94%, white);
  box-shadow: var(--app-shadow-soft);
}

.header-right__label {
  color: var(--app-text-muted);
  font-size: 10px;
  font-weight: 700;
  font-family: var(--app-font-mono);
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.header-right__identity strong {
  font-size: 15px;
  font-weight: 700;
}

.header-right__role {
  color: var(--app-text-secondary);
  font-size: 12px;
}

.header-right__action {
  color: var(--app-primary-dark);
  letter-spacing: 0.04em;
}

.app-body {
  min-height: calc(100vh - var(--app-header-height));
}

.app-aside {
  position: relative;
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: var(--app-space-4);
  padding: var(--app-space-5) var(--app-space-4);
  background: linear-gradient(180deg, color-mix(in srgb, var(--app-surface) 96%, white), color-mix(in srgb, var(--app-surface-muted) 86%, white));
  border-right: 1px solid color-mix(in srgb, var(--app-border) 88%, white);
}

.app-aside::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.36), transparent 34%),
    radial-gradient(circle at top left, color-mix(in srgb, var(--app-primary) 8%, transparent), transparent 32%);
  opacity: 0.8;
}

.app-aside__caption {
  position: relative;
  z-index: 1;
  color: var(--app-text-muted);
  font-size: 11px;
  font-weight: 700;
  font-family: var(--app-font-mono);
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.app-aside__footer {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 6px;
  padding: var(--app-space-4);
  border: 1px solid color-mix(in srgb, var(--app-border) 90%, white);
  border-radius: var(--app-radius-lg);
  background: linear-gradient(180deg, color-mix(in srgb, var(--app-primary-soft) 48%, white), white);
  box-shadow: var(--app-shadow-soft);
}

.app-aside__footer-label {
  color: var(--app-text-muted);
  font-family: var(--app-font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.app-aside__footer strong {
  color: var(--app-text-primary);
  font-size: 13px;
  letter-spacing: 0.04em;
}

.app-menu {
  position: relative;
  z-index: 1;
  border-right: none;
}

.app-menu__index {
  min-width: 24px;
  color: color-mix(in srgb, var(--app-text-muted) 74%, white);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.app-menu :deep(.el-menu-item) {
  height: 52px;
  margin-bottom: 8px;
  border: 1px solid color-mix(in srgb, var(--app-border) 92%, white);
  border-radius: var(--app-radius-lg);
  color: var(--app-text-primary);
  background: color-mix(in srgb, var(--app-surface) 92%, white);
  transition: all var(--app-transition);
}

.app-menu :deep(.el-menu-item .el-icon) {
  margin-right: 10px;
}

.app-menu :deep(.el-menu-item:hover) {
  background: linear-gradient(90deg, color-mix(in srgb, var(--app-primary-soft) 64%, white), color-mix(in srgb, var(--app-warning) 10%, white));
  border-color: color-mix(in srgb, var(--app-primary) 26%, white);
  transform: translateX(2px);
}

.app-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, color-mix(in srgb, var(--app-primary) 18%, white), color-mix(in srgb, var(--app-warning) 14%, white));
  border-color: color-mix(in srgb, var(--app-primary) 42%, white);
  box-shadow: var(--app-shadow-soft);
}

.app-menu :deep(.el-menu-item.is-active .app-menu__index) {
  color: var(--app-primary-dark);
}

.app-main {
  padding: var(--app-space-6);
}

.app-main__frame {
  position: relative;
  min-height: 100%;
  padding: var(--app-space-6) var(--app-space-5) var(--app-space-5);
  border: 1px solid color-mix(in srgb, var(--app-border) 88%, white);
  border-radius: var(--app-radius-xl);
  background: linear-gradient(180deg, color-mix(in srgb, var(--app-surface) 96%, white), color-mix(in srgb, var(--app-surface-muted) 74%, white));
  box-shadow: var(--app-shadow-panel);
}

.app-main__frame::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: inherit;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.5), transparent 24%),
    radial-gradient(circle at top right, color-mix(in srgb, var(--app-primary) 10%, transparent), transparent 26%);
  opacity: 0.85;
}

.app-main__frame::after {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 2px;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--app-primary), color-mix(in srgb, var(--app-warning) 70%, white), transparent 76%);
  opacity: 0.72;
}

.app-main__frame-label {
  position: absolute;
  top: 14px;
  left: var(--app-space-5);
  z-index: 1;
  color: var(--app-text-muted);
  font-family: var(--app-font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.app-main__viewport {
  position: relative;
  z-index: 1;
  min-height: 100%;
}

@media (max-width: 960px) {
  .app-header,
  .header-right {
    gap: var(--app-space-3);
  }

  .app-body {
    flex-direction: column;
  }

  .app-aside {
    width: auto !important;
    border-right: none;
    border-bottom: 1px solid color-mix(in srgb, var(--app-border) 60%, transparent);
  }

  .app-aside__footer {
    display: none;
  }

  .app-menu {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
    gap: 8px;
  }

  .app-menu :deep(.el-menu-item) {
    margin-bottom: 0;
  }

  .app-main {
    padding: var(--app-space-4);
  }

  .app-main__frame {
    padding-top: var(--app-space-5);
  }
}

@media (max-width: 768px) {
  .app-header {
    flex-direction: column;
    align-items: stretch;
    justify-content: center;
    min-height: auto;
    padding: var(--app-space-4);
  }

  .header-right,
  .header-right__identity {
    justify-items: start;
  }

  .app-main {
    padding: var(--app-space-3);
  }

  .app-main__frame {
    padding: var(--app-space-5) var(--app-space-4) var(--app-space-4);
  }

  .app-aside {
    padding: var(--app-space-4) var(--app-space-3);
  }

  .app-menu {
    grid-template-columns: 1fr;
  }
}
</style>
