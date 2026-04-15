import request from './request'

export type LiveLocation = {
  routeId: number
  driverId: number
  lat: number | string
  lng: number | string
  speed?: number | string | null
  recordedAt: string
}

export type TrackPoint = {
  lat: number | string
  lng: number | string
}

export interface LocationReportPayload {
  routeId: number
  lat: number
  lng: number
  speed?: number
  heading?: number
  recordedAt: string
}

export const trackingApi = {
  getLiveLocations() {
    return request.get<unknown, { data: LiveLocation[] }>('/tracking/live')
  },
  getTrack(routeId: number) {
    return request.get<unknown, { data: TrackPoint[] }>(`/tracking/routes/${routeId}/track`)
  },
  reportLocation(data: LocationReportPayload) {
    return request.post('/tracking/location', data)
  },
}
