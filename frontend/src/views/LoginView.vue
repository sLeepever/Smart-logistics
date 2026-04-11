<template>
  <div class="login-container">
    <div class="login-backdrop" />

    <section class="login-shell">
      <div class="login-shell__intro">
        <span class="login-shell__eyebrow">轻盈物流协作空间</span>
        <h1 class="login-shell__title">智慧物流调度系统</h1>
        <p class="login-shell__subtitle">
          统一处理订单审核、调度安排、车辆追踪与司机任务，让整套流程更清晰顺畅。
        </p>

        <div class="login-shell__status-grid">
          <article class="login-shell__status-card">
            <span class="login-shell__status-label">工作范围</span>
            <strong class="login-shell__status-value">订单、调度、履约</strong>
          </article>
          <article class="login-shell__status-card">
            <span class="login-shell__status-label">适用角色</span>
            <strong class="login-shell__status-value">管理员、调度员、司机</strong>
          </article>
        </div>
      </div>

      <div class="login-box">
        <div class="login-header">
          <div>
            <span class="login-header__eyebrow">欢迎回来</span>
            <h2>登录系统</h2>
          </div>
          <el-tag type="success" effect="plain">演示环境</el-tag>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              clearable
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              class="login-form__submit"
              :loading="loading"
              @click="handleLogin"
            >
              进入系统
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-hint">
          演示账号：admin / dispatcher01 / driver001 &nbsp;|&nbsp; 密码：Demo@1234
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { User, Lock } from '@element-plus/icons-vue'
import type { FormInstance } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await authStore.login(form)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-container {
  position: relative;
  isolation: isolate;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  padding: 32px;
  background:
    radial-gradient(circle at top left, color-mix(in srgb, var(--app-warning) 14%, transparent), transparent 22%),
    radial-gradient(circle at top right, color-mix(in srgb, var(--app-primary) 16%, transparent), transparent 28%),
    linear-gradient(180deg, #fbfdfc 0%, #f1f7f4 100%);
}

.login-backdrop {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, var(--app-grid-line) 0 1px, transparent 1px 100%),
    linear-gradient(180deg, var(--app-grid-line) 0 1px, transparent 1px 100%);
  background-size: 20px 20px;
  opacity: 0.3;
}

.login-shell {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(380px, 420px);
  gap: 32px;
  width: min(1120px, 100%);
  align-items: stretch;
}

.login-shell__intro,
.login-box {
  position: relative;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--app-border) 92%, white);
  border-radius: var(--app-radius-xl);
  box-shadow: var(--app-shadow-panel);
  backdrop-filter: blur(18px);
}

.login-shell__intro {
  display: grid;
  align-content: space-between;
  min-height: 560px;
  padding: 40px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--app-primary-soft) 52%, white)),
    linear-gradient(135deg, color-mix(in srgb, var(--app-primary) 9%, transparent), transparent 58%),
    linear-gradient(180deg, color-mix(in srgb, var(--app-warning) 5%, transparent), transparent 50%);
}

.login-shell__intro::before,
.login-box::before {
  content: '';
  position: absolute;
  inset: 0 0 auto 0;
  height: 3px;
  background: var(--app-panel-stripe);
  opacity: 0.92;
}

.login-shell__intro::after,
.login-box::after {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    linear-gradient(90deg, var(--app-grid-line) 0 1px, transparent 1px 100%),
    linear-gradient(180deg, var(--app-grid-line) 0 1px, transparent 1px 100%);
  background-size: 18px 18px;
  opacity: 0.16;
}

.login-shell__eyebrow,
.login-header__eyebrow,
.login-shell__status-label {
  color: var(--app-text-muted);
  font-family: var(--app-font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.2em;
  text-transform: uppercase;
}

.login-shell__title {
  margin-top: 20px;
  color: var(--app-text-strong);
  font-size: clamp(32px, 4vw, 48px);
  font-weight: 700;
  letter-spacing: 0.08em;
  line-height: 1.1;
}

.login-shell__subtitle {
  max-width: 520px;
  margin-top: 20px;
  color: var(--app-text-secondary);
  font-size: 15px;
  line-height: 1.9;
}

.login-shell__status-grid {
  display: grid;
  gap: 16px;
}

.login-shell__status-card {
  padding: 18px 20px;
  border: 1px solid color-mix(in srgb, var(--app-border) 90%, white);
  border-radius: var(--app-radius-md);
  background: rgba(255, 255, 255, 0.84);
  box-shadow: var(--app-shadow-soft);
}

.login-shell__status-value {
  display: block;
  margin-top: 10px;
  color: var(--app-text-strong);
  font-size: 20px;
  font-weight: 700;
}

.login-box {
  padding: 36px 32px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--app-surface-muted) 82%, white)),
    linear-gradient(135deg, color-mix(in srgb, var(--app-warning) 10%, transparent), transparent 60%),
    linear-gradient(180deg, color-mix(in srgb, var(--app-primary) 5%, transparent), transparent 55%);
}

.login-header {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 28px;
}

.login-header h2 {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 700;
  color: var(--app-text-strong);
  letter-spacing: 0.06em;
}

.login-form {
  position: relative;
  z-index: 1;
}

.login-form__submit {
  width: 100%;
}

.login-hint {
  position: relative;
  z-index: 1;
  margin-top: 16px;
  padding-top: 18px;
  border-top: 1px dashed color-mix(in srgb, var(--app-border) 92%, black);
  font-size: 12px;
  color: var(--app-text-muted);
  line-height: 1.7;
}

.login-box :deep(.el-tag) {
  border-color: color-mix(in srgb, var(--app-success) 38%, white);
  border-radius: 999px;
  background: color-mix(in srgb, var(--app-success) 14%, white);
  color: var(--app-success);
  font-weight: 700;
  letter-spacing: 0.04em;
}

.login-box :deep(.el-input__wrapper) {
  border: 1px solid color-mix(in srgb, var(--app-border) 92%, white);
  border-radius: var(--app-radius-sm);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--app-shadow-inset);
}

.login-box :deep(.el-input__inner) {
  color: var(--app-text-primary);
}

.login-box :deep(.el-input__wrapper.is-focus) {
  border-color: color-mix(in srgb, var(--app-primary) 72%, white);
}

.login-box :deep(.el-input__prefix-inner) {
  color: var(--app-text-muted);
}

.login-box :deep(.el-button--primary) {
  border-color: color-mix(in srgb, var(--app-primary-dark) 72%, white);
  background: linear-gradient(180deg, color-mix(in srgb, var(--app-primary) 82%, white), var(--app-primary-dark));
  color: var(--app-text-strong);
  box-shadow: 0 14px 28px rgba(79, 143, 132, 0.24);
}

.login-box :deep(.el-form-item__error) {
  color: color-mix(in srgb, var(--app-danger) 82%, white);
}

@media (max-width: 900px) {
  .login-container {
    padding: 20px;
  }

  .login-shell {
    grid-template-columns: 1fr;
  }

  .login-shell__intro {
    min-height: auto;
  }
}

@media (max-width: 640px) {
  .login-box,
  .login-shell__intro {
    padding: 24px 20px;
  }

  .login-header {
    flex-direction: column;
  }
}
</style>
