import request from './request'
import type { PageResult } from './order'

export interface CustomerProfile {
  contactName?: string
  companyName?: string
  defaultAddress?: string
  remark?: string
}

export interface User {
  id: number
  username: string
  realName: string
  phone: string
  role: string
  status: number
  createdAt: string
  updatedAt: string
  customerProfile?: CustomerProfile | null
}

export interface UserQuery {
  page: number
  size: number
  role?: string
  keyword?: string
}

export interface UserForm {
  username: string
  password?: string
  realName?: string
  phone?: string
  role: string
  status?: number
  contactName?: string
  companyName?: string
  defaultAddress?: string
  remark?: string
}

export interface UserBrief {
  id: number
  username: string
  realName: string
  phone: string
}

export const userApi = {
  list(params: UserQuery) {
    return request.get<unknown, { data: PageResult<User> }>('/users', { params })
  },
  getById(id: number) {
    return request.get<unknown, { data: User }>(`/users/${id}`)
  },
  create(data: UserForm) {
    return request.post<unknown, { data: User }>('/users', data)
  },
  update(id: number, data: UserForm) {
    return request.put<unknown, { data: User }>(`/users/${id}`, data)
  },
  delete(id: number) {
    return request.delete(`/users/${id}`)
  },
  resetPassword(id: number, newPassword: string) {
    return request.patch(`/users/${id}/password`, { newPassword })
  },
  getBrief(id: number) {
    return request.get<unknown, { data: UserBrief }>(`/users/${id}/brief`)
  },
}
