import axios from 'axios'
import type { InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 请求拦截器：自动携带 token
request.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 并发 401 去重：只触发一次 refresh，其他请求排队等待
let isRefreshing = false
let pendingQueue: Array<(token: string) => void> = []

function processQueue(newToken: string) {
  pendingQueue.forEach((resolve) => resolve(newToken))
  pendingQueue = []
}

// 响应拦截器：处理错误，401 时自动刷新 token
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  async (error) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    if (error.response?.status === 401 && !originalRequest._retry) {
      const refreshToken = sessionStorage.getItem('refreshToken')

      if (!refreshToken) {
        const authStore = useAuthStore()
        authStore.logout()
        ElMessage.error('会话已过期，请重新登录')
        return Promise.reject(error)
      }

      if (isRefreshing) {
        // 排队等待 refresh 完成后重放
        return new Promise((resolve) => {
          pendingQueue.push((token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`
            resolve(request(originalRequest))
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const res = await axios.post('/api/auth/refresh', { refreshToken })
        const newAccessToken = res.data?.data?.accessToken
        if (!newAccessToken) throw new Error('refresh failed')
        sessionStorage.setItem('accessToken', newAccessToken)
        processQueue(newAccessToken)
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
        return request(originalRequest)
      } catch {
        pendingQueue = []
        const authStore = useAuthStore()
        authStore.logout()
        ElMessage.error('会话已过期，请重新登录')
        return Promise.reject(error)
      } finally {
        isRefreshing = false
      }
    }

    if (error.response?.status !== 401) {
      ElMessage.error(error.response?.data?.message || '网络错误')
    }
    return Promise.reject(error)
  },
)

export default request
