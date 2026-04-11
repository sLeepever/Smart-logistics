<template>
  <div class="tracking-view app-page">
    <el-card class="tracking-view__panel app-console-panel" shadow="never">
      <template #header>
        <div class="tracking-view__header">
          <div>
            <span class="tracking-view__title">追踪中心</span>
            <p class="tracking-view__subtitle">保持原有刷新与轨迹选择逻辑，让车辆列表与地图切换更清晰。</p>
          </div>

          <div class="tracking-view__actions">
            <el-tag :type="wsConnected ? 'success' : 'danger'" size="small">
              {{ wsConnected ? '实时同步中' : '等待同步' }}
            </el-tag>
            <el-button size="small" @click="loadLive">刷新位置</el-button>
          </div>
        </div>
      </template>

      <el-row :gutter="16" class="tracking-view__layout">
        <el-col :xs="24" :lg="8">
          <section class="tracking-view__side-panel">
            <div class="tracking-view__side-header">
              <span class="tracking-view__side-title">在途车辆（{{ liveList.length }}）</span>
              <span class="tracking-view__side-hint">选择路线即可查看轨迹</span>
            </div>

            <el-scrollbar height="540px">
              <div
                v-for="item in liveList"
                :key="item.routeId"
                class="vehicle-card"
                :class="{ active: selectedRouteId === item.routeId }"
                @click="selectRoute(item)"
              >
                <div class="vehicle-card__top">
                  <div>
                    <span class="vehicle-card__route">路线 #{{ item.routeId }}</span>
                    <p class="vehicle-card__driver">司机编号：{{ item.driverId }}</p>
                  </div>
                  <el-tag type="success" size="small">在途</el-tag>
                </div>

                <div class="vehicle-card__grid">
                  <div class="vehicle-card__metric">
                    <span>位置</span>
                    <strong>{{ Number(item.lat).toFixed(4) }}, {{ Number(item.lng).toFixed(4) }}</strong>
                  </div>
                  <div class="vehicle-card__metric">
                    <span>速度</span>
                    <strong>{{ item.speed ?? '--' }} km/h</strong>
                  </div>
                </div>

                <div class="vehicle-card__footer">更新：{{ formatTime(item.recordedAt) }}</div>
              </div>

              <el-empty v-if="liveList.length === 0" description="暂无在途车辆" :image-size="60" />
            </el-scrollbar>
          </section>
        </el-col>

        <el-col :xs="24" :lg="16">
          <section class="tracking-view__map-frame">
            <div class="tracking-view__map-header">
              <div>
                <span class="tracking-view__map-title">路线地图</span>
                <p class="tracking-view__map-hint">仓库与车辆标记沿用原逻辑，轨迹线仍按地图规划结果绘制。</p>
              </div>
              <el-tag effect="plain">{{ selectedRouteId ? `路线 #${selectedRouteId}` : '未选择路线' }}</el-tag>
            </div>
            <div id="tracking-map" class="tracking-view__map" />
          </section>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { trackingApi, type LiveLocation, type TrackPoint } from '@/api/tracking'

type LeafletIconDefaultPrototype = typeof L.Icon.Default.prototype & {
  _getIconUrl?: string
}

type TrackingSocketMessage = Partial<LiveLocation> & {
  type?: string
  routeId?: number
}

delete (L.Icon.Default.prototype as LeafletIconDefaultPrototype)._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
})

const DEPOT = { lat: 23.0452, lng: 113.3960 }

const liveList = ref<LiveLocation[]>([])
const selectedRouteId = ref<number | null>(null)
const wsConnected = ref(false)

let map: L.Map | null = null
let ws: WebSocket | null = null
const markers = new Map<number, L.Marker>()
const trackLines = new Map<number, L.Polyline>()

function formatTime(t: string) {
  return t ? t.replace('T', ' ').substring(0, 16) : '--'
}

function getCssVar(name: string, fallback: string) {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback
}

function initMap() {
  map = L.map('tracking-map').setView([DEPOT.lat, DEPOT.lng], 11)
  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    attribution: '© 高德地图',
    subdomains: ['1', '2', '3', '4'],
    maxZoom: 18,
  }).addTo(map)

  L.marker([DEPOT.lat, DEPOT.lng], {
    icon: L.divIcon({
      html: `<div class="map-pin depot-pin">
               <div class="pin-head">🏭</div>
               <div class="pin-tail"></div>
             </div>`,
      iconAnchor: [18, 42],
      className: '',
    }),
  }).addTo(map).bindPopup('<b>配送仓库</b><br>广东工业大学大学城校区')
}

async function loadLive() {
  try {
    const res = await trackingApi.getLiveLocations()
    liveList.value = res.data
    updateMarkers(res.data)
  } catch {
    // ignore
  }
}

function updateMarkers(locations: LiveLocation[]) {
  if (!map) return
  locations.forEach(loc => {
    const lat = Number(loc.lat)
    const lng = Number(loc.lng)
    if (markers.has(loc.routeId)) {
      markers.get(loc.routeId)!.setLatLng([lat, lng])
    } else {
      const m = L.marker([lat, lng], {
        icon: L.divIcon({
          html: `<div class="map-pin vehicle-pin">
                   <div class="pin-head">🚚</div>
                   <div class="pin-tail"></div>
                 </div>`,
          iconAnchor: [18, 42],
          className: '',
        }),
      })
        .addTo(map!)
        .bindPopup(`路线 #${loc.routeId}<br>司机编号: ${loc.driverId}<br>速度: ${loc.speed ?? '--'} km/h`)
      markers.set(loc.routeId, m)
    }
  })
}

const GAODE_KEY = import.meta.env.VITE_AMAP_KEY as string

async function fetchRoadRoute(waypoints: [number, number][]): Promise<[number, number][]> {
  if (waypoints.length < 2) return waypoints
  const start = waypoints[0]
  const end = waypoints[waypoints.length - 1]
  if (!start || !end) return waypoints
  const origin = `${start[1]},${start[0]}`
  const destination = `${end[1]},${end[0]}`
  const mid = waypoints.slice(1, -1)
  const waypointStr = mid.map(p => `${p[1]},${p[0]}`).join(';')
  const url = `/gaode-api/v3/direction/driving?origin=${origin}&destination=${destination}${waypointStr ? '&waypoints=' + waypointStr : ''}&key=${GAODE_KEY}&output=json`
  const resp = await fetch(url)
  const data = await resp.json()
  if (data.status !== '1' || !data.route?.paths?.length) return waypoints
  const polyPoints: [number, number][] = []
  for (const step of data.route.paths[0].steps) {
    for (const seg of step.polyline.split(';')) {
      const [lng, lat] = seg.split(',').map(Number)
      if (!isNaN(lat) && !isNaN(lng)) polyPoints.push([lat, lng])
    }
  }
  return polyPoints.length > 0 ? polyPoints : waypoints
}

async function selectRoute(item: LiveLocation) {
  selectedRouteId.value = item.routeId
  try {
    const res = await trackingApi.getTrack(item.routeId)
    const gpsPoints: [number, number][] = res.data.map((point: TrackPoint) => [Number(point.lat), Number(point.lng)])

    trackLines.forEach(line => map?.removeLayer(line))
    trackLines.clear()

    const allWaypoints: [number, number][] = [[DEPOT.lat, DEPOT.lng], ...gpsPoints]
    if (allWaypoints.length < 2) return

    const roadPoints = await fetchRoadRoute(allWaypoints)
    const line = L.polyline(roadPoints, {
      color: getCssVar('--app-primary', '#b8814f'),
      weight: 4,
      opacity: 0.88,
    }).addTo(map!)
    trackLines.set(item.routeId, line)
    map?.fitBounds(line.getBounds(), { padding: [40, 40] })
  } catch {
    // ignore
  }
}

function connectWS() {
  ws = new WebSocket('ws://localhost:5173/ws/tracking')
  ws.onopen = () => { wsConnected.value = true }
  ws.onclose = () => {
    wsConnected.value = false
    setTimeout(connectWS, 3000)
  }
  ws.onerror = () => { wsConnected.value = false }
  ws.onmessage = (e) => {
    try {
      const msg = JSON.parse(e.data) as TrackingSocketMessage
      if (msg.type === 'location' && typeof msg.routeId === 'number') {
        const idx = liveList.value.findIndex(l => l.routeId === msg.routeId)
        const nextLocation: LiveLocation = {
          driverId: msg.driverId ?? 0,
          lat: msg.lat ?? 0,
          lng: msg.lng ?? 0,
          recordedAt: msg.recordedAt ?? '',
          routeId: msg.routeId,
          speed: msg.speed,
        }
        if (idx >= 0) {
          liveList.value[idx] = { ...liveList.value[idx], ...nextLocation }
        } else {
          liveList.value.push(nextLocation)
        }
        updateMarkers([nextLocation])
      }
    } catch {
      // ignore invalid payloads
    }
  }
}

onMounted(async () => {
  await nextTick()
  initMap()
  await loadLive()
  connectWS()
})

onUnmounted(() => {
  ws?.close()
  map?.remove()
})
</script>

<style scoped>
:deep(.map-pin) {
  display: flex;
  flex-direction: column;
  align-items: center;
  filter: drop-shadow(0 6px 12px rgba(79, 143, 132, 0.18));
}

:deep(.pin-head) {
  width: 38px;
  height: 38px;
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  line-height: 1;
}

:deep(.depot-pin .pin-head) {
  background: linear-gradient(180deg, color-mix(in srgb, var(--app-primary) 74%, white), var(--app-primary-dark));
  border: 2px solid rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 18px rgba(79, 143, 132, 0.18);
}

:deep(.vehicle-pin .pin-head) {
  background: linear-gradient(180deg, color-mix(in srgb, var(--app-warning) 84%, white), color-mix(in srgb, var(--app-warning) 62%, white));
  border: 2px solid rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 18px rgba(217, 179, 124, 0.18);
}

:deep(.pin-head) > * {
  transform: rotate(45deg);
}

:deep(.pin-tail) {
  width: 2px;
  height: 8px;
  background: color-mix(in srgb, var(--app-border-strong) 76%, white);
}

:deep(.leaflet-container) {
  font-family: inherit;
  background: var(--app-surface-muted);
}

:deep(.leaflet-tile-pane) {
  filter: saturate(1.04) brightness(1.03);
}

:deep(.leaflet-control-zoom a) {
  border-color: color-mix(in srgb, var(--app-border) 88%, white);
  background: rgba(255, 255, 255, 0.94);
  color: var(--app-text-primary);
}

:deep(.leaflet-control-zoom a:hover) {
  background: color-mix(in srgb, var(--app-primary-soft) 64%, white);
}

:deep(.leaflet-popup-content-wrapper),
:deep(.leaflet-popup-tip) {
  background: rgba(255, 255, 255, 0.96);
  color: var(--app-text-primary);
}

:deep(.leaflet-control-attribution) {
  background: rgba(255, 255, 255, 0.78);
  color: var(--app-text-muted);
}

:deep(.leaflet-control-attribution a) {
  color: var(--app-primary-dark);
}

.tracking-view__header,
.tracking-view__actions,
.tracking-view__side-header,
.tracking-view__map-header,
.vehicle-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--app-space-4);
}

.tracking-view__title,
.tracking-view__side-title,
.tracking-view__map-title {
  display: block;
  font-size: 16px;
  font-weight: 700;
  color: var(--app-text-strong);
  letter-spacing: 0.04em;
}

.tracking-view__subtitle,
.tracking-view__side-hint,
.tracking-view__map-hint,
.vehicle-card__driver,
.vehicle-card__footer,
.vehicle-card__metric span {
  color: var(--app-text-secondary);
  font-size: 12px;
}

.tracking-view__subtitle,
.tracking-view__map-hint {
  margin-top: 6px;
}

.tracking-view__side-panel,
.tracking-view__map-frame {
  height: 100%;
  padding: var(--app-space-4);
  border: 1px solid color-mix(in srgb, var(--app-border) 90%, white);
  border-radius: var(--app-radius-lg);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--app-surface-muted) 82%, white)),
    linear-gradient(135deg, color-mix(in srgb, var(--app-primary) 7%, transparent), transparent 62%);
}

.tracking-view__side-panel {
  display: grid;
  gap: var(--app-space-4);
}

.tracking-view__map-frame {
  display: grid;
  gap: var(--app-space-4);
}

.tracking-view__map {
  height: 540px;
  border: 1px solid color-mix(in srgb, var(--app-border-strong) 88%, white);
  border-radius: var(--app-radius-md);
  overflow: hidden;
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.46);
}

.vehicle-card {
  margin-bottom: var(--app-space-3);
  padding: var(--app-space-4);
  border: 1px solid color-mix(in srgb, var(--app-border) 90%, white);
  border-radius: var(--app-radius-md);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), color-mix(in srgb, var(--app-surface-muted) 76%, white));
  cursor: pointer;
  transition: transform var(--app-transition), border-color var(--app-transition), box-shadow var(--app-transition), background var(--app-transition);
}

.vehicle-card:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--app-primary) 68%, white);
  box-shadow: var(--app-shadow-card);
}

.vehicle-card.active {
  border-color: color-mix(in srgb, var(--app-primary) 78%, white);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.98), color-mix(in srgb, var(--app-primary-soft) 52%, white)),
    linear-gradient(90deg, color-mix(in srgb, var(--app-warning) 14%, transparent), color-mix(in srgb, var(--app-primary) 10%, transparent) 68%, transparent 100%);
}

.vehicle-card__route {
  font-size: 16px;
  font-weight: 700;
  color: var(--app-text-strong);
  letter-spacing: 0.04em;
}

.vehicle-card__driver {
  margin-top: 6px;
}

.vehicle-card__grid {
  display: grid;
  gap: var(--app-space-3);
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: var(--app-space-4);
}

.vehicle-card__metric {
  padding: 12px;
  border-radius: var(--app-radius-sm);
  background: color-mix(in srgb, var(--app-primary-soft) 42%, white);
  border: 1px solid color-mix(in srgb, var(--app-border) 82%, white);
}

.vehicle-card__metric strong {
  display: block;
  margin-top: 6px;
  color: var(--app-text-strong);
  font-family: var(--app-font-mono);
  font-size: 13px;
  line-height: 1.5;
}

.vehicle-card__footer {
  margin-top: var(--app-space-4);
  padding-top: var(--app-space-3);
  border-top: 1px dashed color-mix(in srgb, var(--app-border) 48%, transparent);
}

@media (max-width: 768px) {
  .tracking-view__header,
  .tracking-view__actions,
  .tracking-view__side-header,
  .tracking-view__map-header,
  .vehicle-card__top {
    align-items: flex-start;
    flex-direction: column;
  }

  .vehicle-card__grid {
    grid-template-columns: 1fr;
  }

  .tracking-view__map {
    height: 420px;
  }
}
</style>
