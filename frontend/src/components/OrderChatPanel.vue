<template>
  <section
    class="order-chat-panel"
    :class="{ 'order-chat-panel--compact': compact }"
    data-testid="order-chat-panel"
  >
    <header class="order-chat-panel__header">
      <div>
        <h3 class="order-chat-panel__title">{{ panelTitle || '订单沟通' }}</h3>
        <p class="order-chat-panel__subtitle">历史消息会自动载入，新的沟通内容会实时更新。</p>
      </div>

      <div class="order-chat-panel__status">
        <el-tag :type="connectionTagType" effect="light" size="small">
          {{ connectionLabel }}
        </el-tag>
        <el-button text size="small" :loading="historyLoading" @click="refreshHistory">刷新记录</el-button>
      </div>
    </header>

    <div class="order-chat-panel__body" v-loading="historyLoading">
      <div
        v-if="messages.length > 0"
        ref="messageListRef"
        class="order-chat-panel__messages"
        data-testid="order-chat-message-list"
      >
        <article
          v-for="message in messages"
          :key="message.id"
          class="order-chat-panel__message"
          :class="{ 'order-chat-panel__message--self': isSelf(message) }"
        >
          <div class="order-chat-panel__meta">
            <span class="order-chat-panel__sender">{{ senderLabel(message) }}</span>
            <span class="order-chat-panel__time">{{ formatTime(message.createdAt) }}</span>
          </div>
          <div class="order-chat-panel__bubble">
            {{ message.content }}
          </div>
        </article>
      </div>

      <el-empty
        v-else
        description="当前订单还没有沟通记录，发送第一条消息即可开始协作。"
        :image-size="64"
      />
    </div>

    <footer class="order-chat-panel__composer">
      <el-input
        v-model="draft"
        type="textarea"
        :rows="compact ? 2 : 3"
        maxlength="500"
        show-word-limit
        resize="none"
        placeholder="输入订单沟通内容，按发送后同步给同订单相关角色。"
        data-testid="order-chat-send-input"
        @keydown.ctrl.enter.prevent="handleSend"
      />

      <div class="order-chat-panel__composer-footer">
        <span class="order-chat-panel__hint">
          {{ wsReady ? '当前消息会优先实时送达' : '实时同步暂不可用，将自动切换为常规发送' }}
        </span>
        <el-button
          type="primary"
          :loading="sending"
          :disabled="!draft.trim()"
          data-testid="order-chat-send-button"
          @click="handleSend"
        >
          发送消息
        </el-button>
      </div>
    </footer>
  </section>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  buildChatWsUrl,
  chatApi,
  type OrderChatMessage,
  type OrderChatWsEvent,
} from '@/api/chat'
import { useChatUnreadStore } from '@/stores/chatUnread'

const props = withDefaults(defineProps<{
  orderId: number
  panelTitle?: string
  compact?: boolean
}>(), {
  panelTitle: '',
  compact: false,
})

const currentUserId = Number(sessionStorage.getItem('userId') || 0)
const currentRole = sessionStorage.getItem('role') || ''
const chatUnreadStore = useChatUnreadStore()

const messages = ref<OrderChatMessage[]>([])
const draft = ref('')
const historyLoading = ref(false)
const sending = ref(false)
const connectionState = ref<'disconnected' | 'connecting' | 'connected' | 'reconnecting'>('disconnected')
const messageListRef = ref<HTMLElement | null>(null)

const connectionLabel = computed(() => {
  const map = {
    disconnected: '暂未同步',
    connecting: '正在同步',
    connected: '实时同步中',
    reconnecting: '重新同步中',
  } as const

  return map[connectionState.value]
})

const connectionTagType = computed<'' | 'success' | 'warning' | 'info'>(() => {
  if (connectionState.value === 'connected') return 'success'
  if (connectionState.value === 'connecting' || connectionState.value === 'reconnecting') return 'warning'
  return 'info'
})

const wsReady = computed(() => connectionState.value === 'connected')

let ws: WebSocket | null = null
let reconnectTimer: ReturnType<typeof window.setTimeout> | null = null
let reconnectEnabled = false
let activeOrderId = 0
let seenMessageIds = new Set<number>()

watch(
  () => props.orderId,
  async (orderId) => {
    await bootstrap(orderId)
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  teardown(true)
})

async function bootstrap(orderId: number) {
  teardown(true)
  activeOrderId = orderId
  resetMessages()
  chatUnreadStore.clearCount(orderId)
  await loadHistory(orderId)
  connectSocket(orderId)
}

async function refreshHistory() {
  await loadHistory(props.orderId)
}

async function loadHistory(orderId: number) {
  historyLoading.value = true
  try {
    const res = await chatApi.getOrderMessages(orderId)
    replaceMessages(res.data)
  } catch {
    ElMessage.error('订单聊天记录加载失败，请稍后重试')
  } finally {
    historyLoading.value = false
  }
}

function replaceMessages(nextMessages: OrderChatMessage[]) {
  seenMessageIds = new Set(nextMessages.map(message => message.id))
  messages.value = [...nextMessages].sort(sortMessages)
  scrollToBottom()
}

function upsertMessage(message: OrderChatMessage) {
  if (seenMessageIds.has(message.id)) {
    messages.value = messages.value.map(item => (item.id === message.id ? message : item)).sort(sortMessages)
    scrollToBottom()
    return
  }

  seenMessageIds.add(message.id)
  messages.value = [...messages.value, message].sort(sortMessages)
  scrollToBottom()
}

function sortMessages(left: OrderChatMessage, right: OrderChatMessage) {
  const leftTime = Date.parse(left.createdAt || '')
  const rightTime = Date.parse(right.createdAt || '')

  if (Number.isFinite(leftTime) && Number.isFinite(rightTime) && leftTime !== rightTime) {
    return leftTime - rightTime
  }

  return left.id - right.id
}

function scrollToBottom() {
  nextTick(() => {
    if (!messageListRef.value) return
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  })
}

function connectSocket(orderId: number) {
  reconnectEnabled = true
  clearReconnectTimer()
  connectionState.value = connectionState.value === 'disconnected' ? 'connecting' : 'reconnecting'

  const socket = new WebSocket(buildChatWsUrl())
  ws = socket

  socket.onopen = () => {
    if (ws !== socket) return
    connectionState.value = 'connected'
    sendSocketEvent({ type: 'subscribe', orderId })
  }

  socket.onmessage = (event) => {
    if (ws !== socket) return

    try {
      const payload = JSON.parse(event.data) as OrderChatWsEvent
      if (payload.type === 'chat' && payload.orderId === activeOrderId) {
        upsertMessage(payload.message)
        return
      }

      if (payload.type === 'error') {
        ElMessage.warning(payload.message || '订单聊天服务返回错误')
      }
    } catch {
      // ignore invalid payloads
    }
  }

  socket.onerror = () => {
    if (ws !== socket) return
    if (connectionState.value !== 'connected') {
      connectionState.value = 'reconnecting'
    }
  }

  socket.onclose = () => {
    if (ws !== socket) return
    ws = null

    if (!reconnectEnabled) {
      connectionState.value = 'disconnected'
      return
    }

    connectionState.value = 'reconnecting'
    reconnectTimer = window.setTimeout(() => {
      connectSocket(orderId)
    }, 3000)
  }
}

function sendSocketEvent(payload: Record<string, unknown>) {
  if (!ws || ws.readyState !== WebSocket.OPEN) return false
  ws.send(JSON.stringify(payload))
  return true
}

function teardown(shouldUnsubscribe: boolean) {
  reconnectEnabled = false
  clearReconnectTimer()

  if (!ws) {
    connectionState.value = 'disconnected'
    return
  }

  if (shouldUnsubscribe && ws.readyState === WebSocket.OPEN && activeOrderId) {
    sendSocketEvent({ type: 'unsubscribe', orderId: activeOrderId })
  }

  ws.close()
  ws = null
  connectionState.value = 'disconnected'
}

function clearReconnectTimer() {
  if (!reconnectTimer) return
  window.clearTimeout(reconnectTimer)
  reconnectTimer = null
}

function resetMessages() {
  seenMessageIds = new Set<number>()
  messages.value = []
  draft.value = ''
}

async function handleSend() {
  const content = draft.value.trim()
  if (!content || sending.value) return

  sending.value = true
  try {
    if (sendSocketEvent({ type: 'chat', orderId: props.orderId, content })) {
      draft.value = ''
      return
    }

    const res = await chatApi.sendOrderMessage(props.orderId, content)
    upsertMessage(res.data)
    draft.value = ''

    if (!reconnectEnabled || !ws) {
      connectSocket(props.orderId)
    }
  } catch {
    ElMessage.error('订单消息发送失败，请稍后重试')
  } finally {
    sending.value = false
  }
}

function senderLabel(message: OrderChatMessage) {
  if (isSelf(message)) {
    return '我'
  }

  return roleLabel(message.senderRole)
}

function roleLabel(role?: string) {
  const map: Record<string, string> = {
    admin: '管理员',
    dispatcher: '调度员',
    driver: '司机',
    customer: '客户',
  }

  return map[role ?? ''] ?? role ?? '参与方'
}

function isSelf(message: OrderChatMessage) {
  if (currentUserId > 0 && message.senderUserId === currentUserId) {
    return true
  }

  return Boolean(currentRole && message.senderRole === currentRole && message.senderUserId === currentUserId)
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').substring(0, 16) : '--'
}
</script>

<style scoped>
.order-chat-panel {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--app-primary-soft) 40%, white)),
    linear-gradient(135deg, color-mix(in srgb, var(--app-warning) 8%, transparent), transparent 58%);
  border: 1px solid color-mix(in srgb, var(--app-border) 90%, white);
  border-radius: var(--app-radius-lg);
  display: grid;
  gap: var(--app-space-4);
  padding: var(--app-space-5);
  box-shadow: var(--app-shadow-card);
}

.order-chat-panel--compact {
  padding: var(--app-space-4);
}

.order-chat-panel__header,
.order-chat-panel__status,
.order-chat-panel__composer-footer,
.order-chat-panel__meta {
  align-items: center;
  display: flex;
  gap: var(--app-space-3);
  justify-content: space-between;
}

.order-chat-panel__header {
  align-items: flex-start;
}

.order-chat-panel__title {
  color: var(--app-text-strong);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.order-chat-panel__subtitle,
.order-chat-panel__time,
.order-chat-panel__hint {
  color: var(--app-text-muted);
  font-size: 12px;
}

.order-chat-panel__subtitle {
  margin-top: 6px;
}

.order-chat-panel__body {
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), color-mix(in srgb, var(--app-surface-muted) 70%, white)),
    linear-gradient(90deg, var(--app-grid-line) 0 1px, transparent 1px 100%),
    linear-gradient(180deg, var(--app-grid-line) 0 1px, transparent 1px 100%);
  background-size: auto, 18px 18px, 18px 18px;
  border: 1px solid color-mix(in srgb, var(--app-border) 88%, white);
  border-radius: var(--app-radius-md);
  min-height: 240px;
  padding: var(--app-space-4);
}

.order-chat-panel--compact .order-chat-panel__body {
  min-height: 180px;
}

.order-chat-panel__messages {
  display: flex;
  flex-direction: column;
  gap: var(--app-space-4);
  max-height: 360px;
  overflow-y: auto;
  padding-right: 4px;
}

.order-chat-panel--compact .order-chat-panel__messages {
  max-height: 280px;
}

.order-chat-panel__message {
  align-self: flex-start;
  display: grid;
  gap: 6px;
  max-width: min(78%, 560px);
}

.order-chat-panel__message--self {
  align-self: flex-end;
}

.order-chat-panel__meta {
  color: var(--app-text-secondary);
  font-family: var(--app-font-mono);
  font-size: 12px;
  justify-content: flex-start;
}

.order-chat-panel__message--self .order-chat-panel__meta {
  justify-content: flex-end;
}

.order-chat-panel__sender {
  font-weight: 600;
  letter-spacing: 0.06em;
}

.order-chat-panel__bubble {
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid color-mix(in srgb, var(--app-border) 88%, white);
  border-radius: 14px 14px 14px 6px;
  box-shadow: var(--app-shadow-soft);
  color: var(--app-text-primary);
  line-height: 1.7;
  padding: 10px 14px;
  white-space: pre-wrap;
  word-break: break-word;
}

.order-chat-panel__message--self .order-chat-panel__bubble {
  background: color-mix(in srgb, var(--app-primary-soft) 68%, white);
  border-color: color-mix(in srgb, var(--app-primary) 28%, white);
  border-radius: 14px 14px 6px 14px;
}

.order-chat-panel__composer {
  display: grid;
  gap: var(--app-space-3);
}

@media (max-width: 768px) {
  .order-chat-panel,
  .order-chat-panel__header,
  .order-chat-panel__status,
  .order-chat-panel__composer-footer {
    gap: var(--app-space-3);
  }

  .order-chat-panel__header,
  .order-chat-panel__status,
  .order-chat-panel__composer-footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .order-chat-panel__message {
    max-width: 100%;
  }
}
</style>
