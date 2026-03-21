import request from './request'

export const dispatchApi = {
  listPlans(params: { page: number; size: number }) {
    return request.get<any, { data: any }>('/dispatch/plans', { params })
  },
  getPlanDetail(id: number) {
    return request.get<any, { data: any }>(`/dispatch/plans/${id}`)
  },
  generatePlan() {
    return request.post<any, { data: any }>('/dispatch/plans/generate')
  },
  confirmPlan(id: number) {
    return request.post(`/dispatch/plans/${id}/confirm`)
  },
}
