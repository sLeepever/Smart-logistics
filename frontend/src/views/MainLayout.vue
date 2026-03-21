<template>
  <el-container style="height: 100vh">
    <el-header class="app-header">
      <span class="logo">智慧物流调度系统</span>
      <div class="header-right">
        <span>{{ authStore.userInfo.realName }}（{{ roleLabel }}）</span>
        <el-button text type="primary" @click="handleLogout" style="margin-left: 16px">
          退出登录
        </el-button>
      </div>
    </el-header>

    <el-container>
      <el-aside width="200px" class="app-aside">
        <el-menu
          :default-active="$route.path"
          router
          background-color="#001529"
          text-color="#ffffffa0"
          active-text-color="#ffffff"
        >
          <el-menu-item index="/dashboard">
            <el-icon><Odometer /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>
          <el-menu-item v-if="canManageOrders" index="/orders">
            <el-icon><List /></el-icon>
            <span>订单管理</span>
          </el-menu-item>
          <el-menu-item v-if="canManageOrders" index="/vehicles">
            <el-icon><Van /></el-icon>
            <span>车辆管理</span>
          </el-menu-item>
          <el-menu-item v-if="canManageOrders" index="/dispatch">
            <el-icon><Calendar /></el-icon>
            <span>调度方案</span>
          </el-menu-item>
          <el-menu-item v-if="canManageOrders" index="/tracking">
            <el-icon><Location /></el-icon>
            <span>实时追踪</span>
          </el-menu-item>
          <el-menu-item index="/stats">
            <el-icon><DataAnalysis /></el-icon>
            <span>数据统计</span>
          </el-menu-item>
          <el-menu-item v-if="isDriver" index="/driver/tasks">
            <el-icon><Van /></el-icon>
            <span>我的任务</span>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Odometer, List, Van, Calendar, Location, DataAnalysis } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const roleLabel = computed(() => {
  const map: Record<string, string> = {
    admin: '管理员',
    dispatcher: '调度员',
    driver: '司机',
  }
  return map[authStore.userInfo.role] || authStore.userInfo.role
})

const canManageOrders = computed(() =>
  ['admin', 'dispatcher'].includes(authStore.userInfo.role),
)
const isDriver = computed(() => authStore.userInfo.role === 'driver')

function handleLogout() {
  authStore.logout()
}
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #1a6bcc;
  color: #fff;
  z-index: 10;
}

.logo {
  font-size: 18px;
  font-weight: 700;
}

.header-right {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.header-right .el-button {
  color: #fff;
}

.app-aside {
  background: #001529;
  height: calc(100vh - 60px);
}

.app-aside .el-menu {
  border-right: none;
  height: 100%;
}

.app-main {
  background: #f0f2f5;
  height: calc(100vh - 60px);
  overflow-y: auto;
}
</style>
