import { ref } from 'vue'
import { dispatchApi, type DriverRouteOffer } from '@/api/dispatch'

export function useDriverOffers() {
  const offers = ref<DriverRouteOffer[]>([])
  const loading = ref(false)

  async function refresh() {
    loading.value = true
    try {
      const res = await dispatchApi.getDriverOffers()
      offers.value = res.data
      return offers.value
    } finally {
      loading.value = false
    }
  }

  async function accept(routeId: number) {
    await dispatchApi.acceptRoute(routeId)
    return refresh()
  }

  async function reject(routeId: number) {
    await dispatchApi.rejectRoute(routeId)
    return refresh()
  }

  return {
    offers,
    loading,
    refresh,
    accept,
    reject,
  }
}
