<template>
  <div class="user-management app-page" data-testid="user-management-page">
    <el-card class="user-management__card app-console-panel" shadow="never">
      <template #header>
        <div class="user-management__header">
          <span class="user-management__title">用户管理</span>
          <el-button
            type="primary"
            :icon="Plus"
            :disabled="!isAdmin"
            data-testid="user-create-button"
            @click="openCreate"
          >
            新建用户
          </el-button>
        </div>
      </template>

        <el-form
          :model="query"
          inline
          class="user-management__toolbar app-toolbar-panel"
          data-testid="user-filter-form"
        >
        <el-form-item label="角色">
          <el-select v-model="query.role" placeholder="全部角色" clearable class="user-management__field--sm">
            <el-option
              v-for="option in roleOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="用户名/姓名/手机号"
            clearable
            class="user-management__field--md"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" data-testid="user-search-button" @click="handleSearch">
            查询
          </el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        class="user-management__table"
        :row-class-name="({ row }) => row.status === 0 ? 'user-management__row--disabled' : ''"
        data-testid="user-table"
      >
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="realName" label="姓名" width="120">
          <template #default="{ row }">
            {{ row.realName || row.customerProfile?.contactName || '--' }}
          </template>
        </el-table-column>
        <el-table-column label="角色" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ getRoleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140">
          <template #default="{ row }">
            {{ row.phone || '--' }}
          </template>
        </el-table-column>
        <el-table-column label="客户资料" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.role === 'customer' && row.customerProfile">
              {{ formatCustomerProfile(row.customerProfile) }}
            </span>
            <span v-else>--</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              :data-testid="`user-edit-${row.id}`"
              @click="openEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="primary"
              size="small"
              :data-testid="`user-reset-password-${row.id}`"
              @click="openResetDialog(row)"
            >
              重置密码
            </el-button>
            <el-popconfirm
              v-if="row.status === 1 && !isCurrentUser(row)"
              title="确认禁用该用户？"
              @confirm="handleDisable(row)"
            >
              <template #reference>
                <el-button link type="warning" size="small" :data-testid="`user-disable-${row.id}`">
                  禁用
                </el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm
              v-if="row.status === 0 && !isCurrentUser(row)"
              title="确认启用该用户？"
              @confirm="handleEnable(row)"
            >
              <template #reference>
                <el-button link type="success" size="small" :data-testid="`user-enable-${row.id}`">
                  启用
                </el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm
              v-if="!isCurrentUser(row)"
              title="确认删除该用户？删除后将无法在列表中恢复。"
              @confirm="handleDelete(row)"
            >
              <template #reference>
                <el-button link type="danger" size="small" :data-testid="`user-delete-${row.id}`">
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        class="user-management__pagination"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="loadData"
      />
    </el-card>

    <el-dialog
      v-model="formVisible"
      :title="editingId ? '编辑用户' : '新建用户'"
      width="680px"
      data-testid="user-form-dialog"
      @closed="handleFormDialogClosed"
    >
      <el-form
        ref="formRef"
        v-loading="dialogLoading"
        :model="form"
        :rules="formRules"
        label-width="96px"
        data-testid="user-form"
      >
        <div class="user-management__dialog-grid">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" />
          </el-form-item>
          <el-form-item v-if="!editingId" label="初始密码" prop="password">
            <el-input v-model="form.password" type="password" show-password autocomplete="new-password" />
          </el-form-item>
          <el-form-item label="姓名" prop="realName">
            <el-input v-model="form.realName" placeholder="客户账号可留空并自动使用联系人" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" placeholder="选填，留空则不提交" />
          </el-form-item>
          <el-form-item label="角色" prop="role">
            <el-select v-model="form.role" :disabled="isEditingCurrentUser" style="width: 100%">
              <el-option
                v-for="option in roleOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio :label="1">启用</el-radio>
              <el-radio :label="0" :disabled="isEditingCurrentUser">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
        </div>

        <template v-if="isCustomerRole">
          <el-divider content-position="left">客户资料</el-divider>
          <div class="user-management__dialog-grid">
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="form.contactName" />
            </el-form-item>
            <el-form-item label="公司名称" prop="companyName">
              <el-input v-model="form.companyName" />
            </el-form-item>
          </div>
          <el-form-item label="默认地址" prop="defaultAddress">
            <el-input v-model="form.defaultAddress" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="form.remark" type="textarea" :rows="2" />
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          data-testid="user-submit-button"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="resetVisible"
      title="重置密码"
      width="420px"
      data-testid="user-reset-password-dialog"
      @closed="handleResetDialogClosed"
    >
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="92px" data-testid="user-reset-password-form">
        <el-form-item label="目标用户">
          <el-input :model-value="resetTargetLabel" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input v-model="resetForm.password" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="resetForm.confirmPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="resetSubmitting"
          data-testid="user-reset-password-submit"
          @click="handleResetPassword"
        >
          确认重置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { userApi, type CustomerProfile, type User, type UserForm } from '@/api/user'
import { roleLabels, useAuthStore, type AppRole } from '@/stores/auth'

type RoleOption = {
  value: AppRole
  label: string
}

type UserFormState = {
  username: string
  password: string
  realName: string
  phone: string
  role: AppRole
  status: 0 | 1
  contactName: string
  companyName: string
  defaultAddress: string
  remark: string
}

const authStore = useAuthStore()

const roleOptions: RoleOption[] = [
  { value: 'admin', label: roleLabels.admin },
  { value: 'dispatcher', label: roleLabels.dispatcher },
  { value: 'driver', label: roleLabels.driver },
  { value: 'customer', label: roleLabels.customer },
]

const isAdmin = computed(() => authStore.userInfo.role === 'admin')
const currentUserId = computed(() => Number(authStore.userInfo.userId))

const loading = ref(false)
const dialogLoading = ref(false)
const submitting = ref(false)
const resetSubmitting = ref(false)
const tableData = ref<User[]>([])
const total = ref(0)
const editingId = ref<number | null>(null)
const formVisible = ref(false)
const resetVisible = ref(false)
const resetTarget = ref<User | null>(null)

const query = reactive({
  page: 1,
  size: 10,
  role: '',
  keyword: '',
})

const formRef = ref<FormInstance>()
const resetFormRef = ref<FormInstance>()

function createDefaultForm(): UserFormState {
  return {
    username: '',
    password: '',
    realName: '',
    phone: '',
    role: 'dispatcher',
    status: 1,
    contactName: '',
    companyName: '',
    defaultAddress: '',
    remark: '',
  }
}

const form = reactive<UserFormState>(createDefaultForm())

const resetForm = reactive({
  password: '',
  confirmPassword: '',
})

const isCustomerRole = computed(() => form.role === 'customer')
const isEditingCurrentUser = computed(() => editingId.value !== null && currentUserId.value === editingId.value)

const formRules: FormRules<UserFormState> = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ validator: validateCreatePassword, trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  contactName: [{ validator: validateCustomerField('请输入联系人'), trigger: 'blur' }],
  defaultAddress: [{ validator: validateCustomerField('请输入默认地址'), trigger: 'blur' }],
}

const resetRules: FormRules<typeof resetForm> = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }],
}

watch(
  () => form.role,
  (role) => {
    if (role !== 'customer') {
      clearCustomerFields()
      formRef.value?.clearValidate(['contactName', 'defaultAddress'])
    }
  },
)

async function loadData() {
  loading.value = true
  try {
    const res = await userApi.list({
      page: query.page,
      size: query.size,
      role: query.role || undefined,
      keyword: query.keyword.trim() || undefined,
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadData()
}

function handleReset() {
  query.page = 1
  query.size = 10
  query.role = ''
  query.keyword = ''
  loadData()
}

function resetFormState() {
  Object.assign(form, createDefaultForm())
}

function clearCustomerFields() {
  form.contactName = ''
  form.companyName = ''
  form.defaultAddress = ''
  form.remark = ''
}

function assignFormFromUser(user: User) {
  Object.assign(form, {
    username: user.username || '',
    password: '',
    realName: user.realName || '',
    phone: user.phone || '',
    role: normalizeRole(user.role),
    status: normalizeStatus(user.status),
    contactName: user.customerProfile?.contactName || '',
    companyName: user.customerProfile?.companyName || '',
    defaultAddress: user.customerProfile?.defaultAddress || '',
    remark: user.customerProfile?.remark || '',
  })
}

function normalizeRole(role: string): AppRole {
  return roleOptions.some(option => option.value === role) ? (role as AppRole) : 'dispatcher'
}

function normalizeStatus(status: number | undefined): 0 | 1 {
  return status === 0 ? 0 : 1
}

async function openCreate() {
  editingId.value = null
  dialogLoading.value = false
  resetFormState()
  formVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

async function openEdit(row: User) {
  editingId.value = row.id
  dialogLoading.value = true
  resetFormState()
  try {
    const res = await userApi.getById(row.id)
    assignFormFromUser(res.data)
    formVisible.value = true
    await nextTick()
    formRef.value?.clearValidate()
  } catch {
    editingId.value = null
    formVisible.value = false
    resetFormState()
  } finally {
    dialogLoading.value = false
  }
}

function buildPayload(includePassword: boolean): UserForm {
  const payload: UserForm = {
    username: normalizeText(form.username) || '',
    realName: normalizeText(form.realName),
    phone: normalizeText(form.phone),
    role: form.role,
    status: form.status,
  }

  if (includePassword) {
    payload.password = normalizeText(form.password) || ''
  }

  if (form.role === 'customer') {
    payload.contactName = normalizeText(form.contactName)
    payload.companyName = normalizeText(form.companyName)
    payload.defaultAddress = normalizeText(form.defaultAddress)
    payload.remark = normalizeText(form.remark)
  }

  return payload
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    const payload = buildPayload(!editingId.value)
    if (editingId.value) {
      await userApi.update(editingId.value, payload)
      ElMessage.success('用户信息已更新')
    } else {
      await userApi.create(payload)
      ElMessage.success('用户创建成功')
    }
    formVisible.value = false
    await loadData()
  } finally {
    submitting.value = false
  }
}

async function handleDisable(row: User) {
  const res = await userApi.getById(row.id)
  await userApi.update(row.id, buildPayloadFromUser(res.data, { status: 0 }))
  ElMessage.success('用户已禁用')
  await loadData()
}

async function handleEnable(row: User) {
  const res = await userApi.getById(row.id)
  await userApi.update(row.id, buildPayloadFromUser(res.data, { status: 1 }))
  ElMessage.success('用户已启用')
  await loadData()
}

async function handleDelete(row: User) {
  await userApi.delete(row.id)
  ElMessage.success('用户已删除')
  if (tableData.value.length === 1 && query.page > 1) {
    query.page -= 1
  }
  await loadData()
}

function openResetDialog(row: User) {
  resetTarget.value = row
  resetVisible.value = true
}

async function handleResetPassword() {
  await resetFormRef.value?.validate()
  if (!resetTarget.value) {
    return
  }

  resetSubmitting.value = true
  try {
    await userApi.resetPassword(resetTarget.value.id, resetForm.password.trim())
    ElMessage.success('密码重置成功')
    resetVisible.value = false
  } finally {
    resetSubmitting.value = false
  }
}

function handleFormDialogClosed() {
  formRef.value?.resetFields()
  editingId.value = null
  dialogLoading.value = false
  resetFormState()
}

function handleResetDialogClosed() {
  resetFormRef.value?.resetFields()
  resetTarget.value = null
  resetForm.password = ''
  resetForm.confirmPassword = ''
}

function normalizeText(value?: string) {
  const normalized = value?.trim()
  return normalized ? normalized : undefined
}

function buildPayloadFromUser(user: User, overrides: Partial<UserForm> = {}): UserForm {
  const payload: UserForm = {
    username: normalizeText(user.username) || '',
    realName: normalizeText(user.realName),
    phone: normalizeText(user.phone),
    role: normalizeRole(user.role),
    status: normalizeStatus(user.status),
  }

  if (payload.role === 'customer') {
    payload.contactName = normalizeText(user.customerProfile?.contactName)
    payload.companyName = normalizeText(user.customerProfile?.companyName)
    payload.defaultAddress = normalizeText(user.customerProfile?.defaultAddress)
    payload.remark = normalizeText(user.customerProfile?.remark)
  }

  return { ...payload, ...overrides }
}

function isCurrentUser(row: User) {
  return currentUserId.value === row.id
}

function getRoleLabel(role: string) {
  return roleLabels[normalizeRole(role)] || role
}

function validateCreatePassword(_: unknown, value: string, callback: (error?: Error) => void) {
  if (editingId.value) {
    callback()
    return
  }
  if (!value?.trim()) {
    callback(new Error('请输入初始密码'))
    return
  }
  if (value.trim().length < 6) {
    callback(new Error('密码至少 6 位'))
    return
  }
  callback()
}

function validatePhone(_: unknown, value: string, callback: (error?: Error) => void) {
  if (!value?.trim()) {
    callback()
    return
  }
  const phonePattern = /^1[3-9]\d{9}$/
  if (!phonePattern.test(value.trim())) {
    callback(new Error('手机号格式不正确'))
    return
  }
  callback()
}

function validateCustomerField(message: string) {
  return (_: unknown, value: string, callback: (error?: Error) => void) => {
    if (form.role !== 'customer') {
      callback()
      return
    }
    if (!value?.trim()) {
      callback(new Error(message))
      return
    }
    callback()
  }
}

function validateConfirmPassword(_: unknown, value: string, callback: (error?: Error) => void) {
  if (!value?.trim()) {
    callback(new Error('请再次输入新密码'))
    return
  }
  if (value.trim() !== resetForm.password.trim()) {
    callback(new Error('两次输入的密码不一致'))
    return
  }
  callback()
}

function formatCustomerProfile(profile?: CustomerProfile | null) {
  if (!profile) {
    return '--'
  }
  return [profile.contactName, profile.companyName, profile.defaultAddress].filter(Boolean).join(' / ') || '--'
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '--'
}

const resetTargetLabel = computed(() => {
  if (!resetTarget.value) {
    return ''
  }
  return `${resetTarget.value.username} / ${resetTarget.value.realName || '--'}`
})

onMounted(loadData)
</script>

<style scoped>
.user-management {
  display: grid;
  gap: var(--app-space-4);
}

.user-management__card {
  overflow: hidden;
}

.user-management__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--app-space-4);
}

.user-management__title {
  font-size: 16px;
  font-weight: 700;
  color: var(--app-text-strong);
  letter-spacing: 0.04em;
}

.user-management__toolbar {
  align-items: end;
}

.user-management__table {
  width: 100%;
}

/* 禁用用户行整行灰显 */
.user-management :deep(.user-management__row--disabled) {
  color: var(--app-text-muted);
  opacity: 0.6;
}

.user-management :deep(.user-management__row--disabled td) {
  background-color: color-mix(in srgb, var(--app-surface-muted) 60%, white) !important;
}

.user-management__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--app-space-4);
}

.user-management__dialog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 var(--app-space-4);
}

.user-management :deep(.el-divider__text) {
  padding: 0 var(--app-space-3);
  color: var(--app-text-muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  background: transparent;
}

.user-management__field--sm {
  width: 136px;
}

.user-management__field--md {
  width: 220px;
}

@media (max-width: 768px) {
  .user-management__header {
    align-items: stretch;
    flex-direction: column;
  }

  .user-management__dialog-grid {
    grid-template-columns: 1fr;
  }

  .user-management__field--sm,
  .user-management__field--md {
    width: 100%;
  }
}
</style>
