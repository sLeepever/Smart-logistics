import request from './request'

export interface Order {
  id: number
  orderNo: string
  senderName: string
  senderPhone: string
  senderAddress: string
  senderLng: number
  senderLat: number
  receiverName: string
  receiverPhone: string
  receiverAddress: string
  receiverLng: number
  receiverLat: number
  goodsName: string
  weight: number
  volume: number
  status: string
  remark: string
  creatorId: number
  createdAt: string
  updatedAt: string
}

export interface OrderReviewRequest {
  action: string
  remark?: string
}

export interface OrderQuery {
  page: number
  size: number
  status?: string
  startDate?: string
  endDate?: string
  keyword?: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export const orderApi = {
  list(params: OrderQuery) {
    return request.get<unknown, { data: PageResult<Order> }>('/orders', { params })
  },
  listMine(params: OrderQuery) {
    return request.get<unknown, { data: PageResult<Order> }>('/orders/mine', { params })
  },
  getById(id: number) {
    return request.get<unknown, { data: Order }>(`/orders/${id}`)
  },
  create(data: Partial<Order>) {
    return request.post<unknown, { data: Order }>('/orders', data)
  },
  update(id: number, data: Partial<Order>) {
    return request.put<unknown, { data: Order }>(`/orders/${id}`, data)
  },
  delete(id: number) {
    return request.delete(`/orders/${id}`)
  },
  cancel(id: number) {
    return request.patch(`/orders/${id}/cancel`)
  },
  cancelPendingReview(id: number) {
    return request.patch(`/orders/${id}/cancel`)
  },
  review(id: number, action: string, remark?: string) {
    const data: OrderReviewRequest = { action, remark }
    return request.patch(`/orders/${id}/review`, data)
  },
  changeStatus(id: number, status: string, remark?: string) {
    return request.patch(`/orders/${id}/status`, { targetStatus: status, remark })
  },
}
