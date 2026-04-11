import request from './request'
import type { PageResult } from './order'

export interface DispatchPlanQuery {
  page: number
  size: number
}

export interface DriverRoute {
  id: number
  planId: number
  vehicleId: number
  driverId: number
  status: string
  estimatedDistance: number | string | null
  estimatedDuration: number | null
  actualDistance?: number | string | null
  startedAt?: string | null
  completedAt?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface DriverRouteStop {
  id: number
  routeId?: number
  orderId: number
  stopSeq: number
  stopType: string
  address: string
  lng?: number | string | null
  lat?: number | string | null
  arrivedAt?: string | null
  createdAt?: string
}

export interface DriverRouteTask {
  route: DriverRoute
  stops: DriverRouteStop[]
}

export interface DriverRouteOffer {
  routeId: number
  planId: number
  vehicleId: number
  routeStatus: string
  candidateStatus: string
  estimatedDistance: number | string | null
  estimatedDuration: number | null
  displayOrder: number | null
  offeredAt: string | null
  detailsVisible: boolean
}

export interface RouteDetail {
  routeId: number
  planId: number
  vehicleId: number
  driverId: number
  status: string
  estimatedDistance: number | string | null
  estimatedDuration: number | null
  detailsVisible: boolean
  stops: DriverRouteStop[]
}

export interface DispatchPlan {
  id: number
  planNo: string
  totalOrders: number
  totalRoutes: number
  beforeTotalDistance: number | null
  afterTotalDistance: number | null
  beforeVehicleCount?: number | null
  afterVehicleCount?: number | null
  status: string
  createdAt: string
}

export interface DispatchPlanRouteView {
  route: DriverRoute
  stops: DriverRouteStop[]
}

export interface DispatchPlanDetail {
  plan: DispatchPlan
  routes: DispatchPlanRouteView[]
}

export const dispatchApi = {
  listPlans(params: DispatchPlanQuery) {
    return request.get<unknown, { data: PageResult<DispatchPlan> }>('/dispatch/plans', { params })
  },
  getPlanDetail(id: number) {
    return request.get<unknown, { data: DispatchPlanDetail }>(`/dispatch/plans/${id}`)
  },
  generatePlan() {
    return request.post<unknown, { data: DispatchPlanDetail }>('/dispatch/plans/generate')
  },
  confirmPlan(id: number) {
    return request.post(`/dispatch/plans/${id}/confirm`)
  },
  getDriverRoutes() {
    return request.get<unknown, { data: DriverRouteTask[] }>('/dispatch/driver/routes')
  },
  getDriverOffers() {
    return request.get<unknown, { data: DriverRouteOffer[] }>('/dispatch/driver/offers')
  },
  acceptRoute(routeId: number) {
    return request.post(`/dispatch/routes/${routeId}/accept`)
  },
  rejectRoute(routeId: number) {
    return request.post(`/dispatch/routes/${routeId}/reject`)
  },
  getRouteDetail(routeId: number) {
    return request.get<unknown, { data: RouteDetail }>(`/dispatch/routes/${routeId}`)
  },
}
