import { ref, onUnmounted } from 'vue'

export interface DriverPosition {
  lat: number
  lng: number
  accuracy: number
  speed: number | null
  heading: number | null
  timestamp: number
}

export type LocationStatus = 'idle' | 'requesting' | 'granted' | 'denied' | 'unavailable'

export function useDriverLocation() {
  const position = ref<DriverPosition | null>(null)
  const status = ref<LocationStatus>('idle')
  const errorMessage = ref('')

  let watchId: number | null = null

  function start() {
    if (!navigator.geolocation) {
      status.value = 'unavailable'
      errorMessage.value = '浏览器不支持地理定位'
      return
    }
    status.value = 'requesting'

    watchId = navigator.geolocation.watchPosition(
      (pos) => {
        status.value = 'granted'
        errorMessage.value = ''
        position.value = {
          lat: pos.coords.latitude,
          lng: pos.coords.longitude,
          accuracy: pos.coords.accuracy,
          speed: pos.coords.speed,
          heading: pos.coords.heading,
          timestamp: pos.timestamp,
        }
      },
      (err) => {
        if (err.code === err.PERMISSION_DENIED) {
          status.value = 'denied'
          errorMessage.value = '位置权限被拒绝，请在浏览器设置中允许定位'
        } else if (err.code === err.POSITION_UNAVAILABLE) {
          status.value = 'unavailable'
          errorMessage.value = '无法获取位置信息'
        } else {
          status.value = 'unavailable'
          errorMessage.value = '位置获取超时，请检查网络'
        }
      },
      {
        enableHighAccuracy: true,
        maximumAge: 10000,
        timeout: 15000,
      },
    )
  }

  function stop() {
    if (watchId !== null) {
      navigator.geolocation.clearWatch(watchId)
      watchId = null
    }
  }

  onUnmounted(stop)

  return { position, status, errorMessage, start, stop }
}
