import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { authApi, type LoginRequest } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const router = useRouter()

  const accessToken = ref(sessionStorage.getItem('accessToken') || '')
  const userInfo = ref({
    userId: Number(sessionStorage.getItem('userId') || 0),
    username: sessionStorage.getItem('username') || '',
    realName: sessionStorage.getItem('realName') || '',
    role: sessionStorage.getItem('role') || '',
  })

  const isLoggedIn = () => !!accessToken.value

  async function login(data: LoginRequest) {
    const res = await authApi.login(data)
    const info = res.data
    accessToken.value = info.accessToken
    userInfo.value = {
      userId: info.userId,
      username: info.username,
      realName: info.realName,
      role: info.role,
    }
    sessionStorage.setItem('accessToken', info.accessToken)
    sessionStorage.setItem('refreshToken', info.refreshToken)
    sessionStorage.setItem('userId', String(info.userId))
    sessionStorage.setItem('username', info.username)
    sessionStorage.setItem('realName', info.realName)
    sessionStorage.setItem('role', info.role)

    // 按角色跳转
    const roleRouteMap: Record<string, string> = {
      admin: '/dashboard',
      dispatcher: '/dashboard',
      driver: '/driver/tasks',
    }
    router.push(roleRouteMap[info.role] || '/dashboard')
  }

  function logout() {
    accessToken.value = ''
    userInfo.value = { userId: 0, username: '', realName: '', role: '' }
    sessionStorage.clear()
    router.push('/login')
  }

  return { accessToken, userInfo, isLoggedIn, login, logout }
})
