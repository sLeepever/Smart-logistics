import request from './request'

export interface OrderChatMessage {
  id: number
  senderUserId: number
  senderRole: string
  content: string
  createdAt: string
}

export interface SendOrderMessagePayload {
  content: string
}

export interface ChatSubscribeEvent {
  type: 'subscribe' | 'unsubscribe'
  orderId: number
}

export interface ChatSendEvent {
  type: 'chat'
  orderId: number
  content: string
}

export interface OrderChatWsMessage {
  type: 'chat'
  orderId: number
  message: OrderChatMessage
}

export interface OrderChatWsError {
  type: 'error'
  code: number
  message: string
}

export type OrderChatWsEvent = OrderChatWsMessage | OrderChatWsError

export function buildChatWsUrl(options: { origin?: string; token?: string } = {}) {
  const origin = options.origin ?? (typeof window !== 'undefined' ? window.location.origin : 'http://localhost')
  const token = options.token ?? (typeof window !== 'undefined' ? sessionStorage.getItem('accessToken') || '' : '')
  const url = new URL('/ws/chat', origin)

  url.protocol = url.protocol === 'https:' ? 'wss:' : 'ws:'
  if (token) {
    url.searchParams.set('token', token)
  }

  return url.toString()
}

export const chatApi = {
  getOrderMessages(orderId: number) {
    return request.get<unknown, { data: OrderChatMessage[] }>(`/chat/orders/${orderId}/messages`)
  },
  sendOrderMessage(orderId: number, content: string) {
    return request.post<unknown, { data: OrderChatMessage }>(`/chat/orders/${orderId}/messages`, { content })
  },
}
