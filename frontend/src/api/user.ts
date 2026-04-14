import request from './request'

export interface UserBrief {
  id: number
  username: string
  realName: string
  phone: string
}

export const userApi = {
  getBrief(id: number) {
    return request.get<unknown, { data: UserBrief }>(`/users/${id}/brief`)
  },
}
