import request from './request'

export interface Vehicle {
  id: number
  plateNo: string
  vehicleType: string
  maxWeight: number
  maxVolume: number
  driverId: number | null
  status: string
  createdAt: string
}

export const vehicleApi = {
  list(params: { page: number; size: number; status?: string }) {
    return request.get<any, { data: any }>('/vehicles', { params })
  },
  create(data: Partial<Vehicle>) {
    return request.post<any, { data: Vehicle }>('/vehicles', data)
  },
  update(id: number, data: Partial<Vehicle>) {
    return request.put<any, { data: Vehicle }>(`/vehicles/${id}`, data)
  },
  delete(id: number) {
    return request.delete(`/vehicles/${id}`)
  },
  changeStatus(id: number, status: string) {
    return request.patch(`/vehicles/${id}/status`, { status })
  },
}
