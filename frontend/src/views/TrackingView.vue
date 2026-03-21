<template>
  <el-card>
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span style="font-weight:600">实时追踪</span>
        <div style="display:flex;align-items:center;gap:12px">
          <el-tag :type="wsConnected ? 'success' : 'danger'" size="small">
            {{ wsConnected ? 'WebSocket 已连接' : 'WebSocket 未连接' }}
          </el-tag>
          <el-button size="small" @click="loadLive">刷新位置</el-button>
        </div>
      </div>
    </template>

    <el-row :gutter="16">
      <!-- 左侧：车辆列表 -->
      <el-col :span="8">
        <div style="font-weight:600;margin-bottom:8px;color:#606266">在途车辆（{{ liveList.length }}）</div>
        <el-scrollbar height="520px">
          <div
            v-for="item in liveList"
            :key="item.routeId"
            class="vehicle-card"
            :class="{ active: selectedRouteId === item.routeId }"
            @click="selectRoute(item)"
          >
            <div style="display:flex;justify-content:space-between">
              <span style="font-weight:600">路线 #{{ item.routeId }}</span>
              <el-tag type="success" size="small">在途</el-tag>
            </div>
            <div style="font-size:12px;color:#909399;margin-top:6px">
              司机ID：{{ item.driverId }}
            </div>
            <div style="font-size:12px;color:#606266;margin-top:4px">
              位置：{{ Number(item.lat).toFixed(4) }}, {{ Number(item.lng).toFixed(4) }}
            </div>
            <div style="font-size:12px;color:#909399;margin-top:4px">
              速度：{{ item.speed ?? '--' }} km/h &nbsp;
              更新：{{ formatTime(item.recordedAt) }}
            </div>
          </div>
          <el-empty v-if="liveList.length === 0" description="暂无在途车辆" :image-size="60" />
        </el-scrollbar>
      </el-col>

      <!-- 右侧：地图 -->
      <el-col :span="16">
        <div id="tracking-map" style="height:540px;border-radius:6px;border:1px solid #e4e7ed" />
      </el-col>
    </el-row>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { trackingApi } from '@/api/tracking'

// 修复 Leaflet 默认图标路径问题
delete (L.Icon.Default.prototype as any)._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
})

const DEPOT = { lat: 23.0452, lng: 113.3960 }

const liveList = ref<any[]>([])
const selectedRouteId = ref<number | null>(null)
const wsConnected = ref(false)

let map: L.Map | null = null
let ws: WebSocket | null = null
const markers = new Map<number, L.Marker>()
const trackLines = new Map<number, L.Polyline>()
let depotMarker: L.Marker | null = null

function formatTime(t: string) {
  return t ? t.replace('T', ' ').substring(0, 16) : '--'
}

// 初始化地图
function initMap() {
  map = L.map('tracking-map').setView([DEPOT.lat, DEPOT.lng], 11)
  L.tileLayer('https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}', {
    attribution: '© 高德地图',
    subdomains: ['1', '2', '3', '4'],
    maxZoom: 18,
  }).addTo(map)

  // 仓库标记
  depotMarker = L.marker([DEPOT.lat, DEPOT.lng], {
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

// 加载当前在途位置
async function loadLive() {
  try {
    const res = await trackingApi.getLiveLocations()
    liveList.value = res.data
    updateMarkers(res.data)
  } catch {
    // ignore
  }
}

function updateMarkers(locations: any[]) {
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
        .bindPopup(`路线 #${loc.routeId}<br>司机ID: ${loc.driverId}<br>速度: ${loc.speed ?? '--'} km/h`)
      markers.set(loc.routeId, m)
    }
  })
}

const GAODE_KEY = 'fbce24aefac90e1cd3f51fe478e79648'

// 调用高德驾车路线API，返回路网折线坐标
async function fetchRoadRoute(waypoints: [number, number][]): Promise<[number, number][]> {
  // waypoints: [[lat, lng], ...]，第一个为起点，最后一个为终点
  if (waypoints.length < 2) return waypoints
  const origin = `${waypoints[0][1]},${waypoints[0][0]}`
  const destination = `${waypoints[waypoints.length - 1][1]},${waypoints[waypoints.length - 1][0]}`
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

// 选中路线，加载历史轨迹（走实际道路）
async function selectRoute(item: any) {
  selectedRouteId.value = item.routeId
  try {
    const res = await trackingApi.getTrack(item.routeId)
    const gpsPoints: [number, number][] = res.data.map((r: any) => [Number(r.lat), Number(r.lng)])

    // 清除旧轨迹
    trackLines.forEach(line => map?.removeLayer(line))
    trackLines.clear()

    // 起点：仓库；途经：GPS记录点
    const allWaypoints: [number, number][] = [[DEPOT.lat, DEPOT.lng], ...gpsPoints]
    if (allWaypoints.length < 2) return

    // 用高德路网折线替代直线
    const roadPoints = await fetchRoadRoute(allWaypoints)
    const line = L.polyline(roadPoints, { color: '#1a6bcc', weight: 4, opacity: 0.85 }).addTo(map!)
    trackLines.set(item.routeId, line)
    map?.fitBounds(line.getBounds(), { padding: [40, 40] })
  } catch {
    // ignore
  }
}

// WebSocket 实时推送
function connectWS() {
  ws = new WebSocket('ws://localhost:5173/ws/tracking')
  ws.onopen = () => { wsConnected.value = true }
  ws.onclose = () => {
    wsConnected.value = false
    // 断线重连
    setTimeout(connectWS, 3000)
  }
  ws.onerror = () => { wsConnected.value = false }
  ws.onmessage = (e) => {
    try {
      const msg = JSON.parse(e.data)
      if (msg.type === 'location') {
        // 更新列表
        const idx = liveList.value.findIndex(l => l.routeId === msg.routeId)
        if (idx >= 0) {
          liveList.value[idx] = { ...liveList.value[idx], ...msg }
        } else {
          liveList.value.push(msg)
        }
        // 更新地图标记
        updateMarkers([msg])
      }
    } catch { /* ignore */ }
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
  filter: drop-shadow(0 2px 4px rgba(0,0,0,0.35));
}
:deep(.pin-head) {
  width: 36px;
  height: 36px;
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  line-height: 1;
}
:deep(.depot-pin .pin-head) {
  background: #1a6bcc;
  border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(26,107,204,0.5);
}
:deep(.vehicle-pin .pin-head) {
  background: #e6a23c;
  border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(230,162,60,0.5);
}
:deep(.pin-head) > * {
  transform: rotate(45deg);
}
:deep(.pin-tail) {
  width: 2px;
  height: 8px;
  background: #888;
  margin-top: 0;
}

.vehicle-card {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.vehicle-card:hover {
  border-color: #1a6bcc;
  background: #f0f7ff;
}
.vehicle-card.active {
  border-color: #1a6bcc;
  background: #ecf5ff;
}
</style>
