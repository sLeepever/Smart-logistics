import request from './request'

export const trackingApi = {
  getLiveLocations() {
    return request.get<any, { data: any[] }>('/tracking/live')
  },
  getTrack(routeId: number) {
    return request.get<any, { data: any[] }>(`/tracking/routes/${routeId}/track`)
  },
}
