import { ref } from 'vue'

const AMAP_KEY = import.meta.env.VITE_AMAP_KEY as string

export interface GeocodeResult {
  lng: number
  lat: number
  address: string
}

export function useGeocode() {
  const geocoding = ref(false)

  async function geocode(address: string): Promise<GeocodeResult | null> {
    if (!address?.trim()) return null
    geocoding.value = true
    try {
      const url = `https://restapi.amap.com/v3/geocode/geo?address=${encodeURIComponent(address)}&city=广州&key=${AMAP_KEY}`
      const res = await fetch(url)
      const data = await res.json()
      if (data.status !== '1' || !data.geocodes?.length) return null
      const [lng, lat] = data.geocodes[0].location.split(',').map(Number)
      return { lng, lat, address: data.geocodes[0].formatted_address ?? address }
    } catch {
      return null
    } finally {
      geocoding.value = false
    }
  }

  return { geocode, geocoding }
}
