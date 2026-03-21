import request from './request'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  userId: number
  username: string
  realName: string
  role: string
}

export const authApi = {
  login: (data: LoginRequest) =>
    request.post<any, { data: LoginResponse }>('/auth/login', data),

  logout: () =>
    request.post('/auth/logout'),

  refresh: (refreshToken: string) =>
    request.post<any, { data: LoginResponse }>('/auth/refresh', { refreshToken }),

  me: () =>
    request.get('/auth/me'),
}
