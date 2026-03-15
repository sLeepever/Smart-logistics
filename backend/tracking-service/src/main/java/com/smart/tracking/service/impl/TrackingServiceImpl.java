package com.smart.tracking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.tracking.dto.LocationReportRequest;
import com.smart.tracking.entity.LocationRecord;
import com.smart.tracking.mapper.LocationRecordMapper;
import com.smart.tracking.service.TrackingService;
import com.smart.tracking.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingServiceImpl implements TrackingService {

    private final LocationRecordMapper locationRecordMapper;
    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    @Override
    public void reportLocation(LocationReportRequest request, Long driverId) {
        LocationRecord record = new LocationRecord();
        record.setRouteId(request.getRouteId());
        record.setDriverId(driverId);
        record.setLng(request.getLng());
        record.setLat(request.getLat());
        record.setSpeed(request.getSpeed());
        record.setHeading(request.getHeading());
        record.setRecordedAt(request.getRecordedAt());
        record.setCreatedAt(LocalDateTime.now());
        locationRecordMapper.insert(record);

        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "type", "location",
                    "routeId", record.getRouteId(),
                    "driverId", record.getDriverId(),
                    "lng", record.getLng(),
                    "lat", record.getLat(),
                    "speed", record.getSpeed() != null ? record.getSpeed() : 0,
                    "heading", record.getHeading() != null ? record.getHeading() : 0,
                    "recordedAt", record.getRecordedAt().toString()
            ));
            sessionManager.broadcast(json);
        } catch (Exception e) {
            log.error("WebSocket 广播位置失败", e);
        }
    }

    @Override
    public List<LocationRecord> getTrack(Long routeId) {
        return locationRecordMapper.selectList(
                new LambdaQueryWrapper<LocationRecord>()
                        .eq(LocationRecord::getRouteId, routeId)
                        .orderByAsc(LocationRecord::getRecordedAt));
    }

    @Override
    public List<LocationRecord> getLiveLocations() {
        return locationRecordMapper.selectLiveLocations();
    }
}
