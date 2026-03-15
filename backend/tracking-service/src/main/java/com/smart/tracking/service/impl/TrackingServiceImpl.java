package com.smart.tracking.service.impl;

import com.smart.tracking.dto.LocationReportRequest;
import com.smart.tracking.entity.LocationRecord;
import com.smart.tracking.mapper.LocationRecordMapper;
import com.smart.tracking.service.TrackingService;
import com.smart.tracking.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingServiceImpl implements TrackingService {

    private final LocationRecordMapper locationRecordMapper;
    private final WebSocketSessionManager sessionManager;

    @Override
    public void reportLocation(LocationReportRequest request, Long driverId) {
        // TODO: 保存位置记录，通过 WebSocket 广播给订阅该路线的调度员
        throw new UnsupportedOperationException("TODO: implement reportLocation");
    }

    @Override
    public List<LocationRecord> getTrack(Long routeId) {
        // TODO: 查询路线历史轨迹，按 recordedAt 升序
        throw new UnsupportedOperationException("TODO: implement getTrack");
    }

    @Override
    public List<LocationRecord> getLiveLocations() {
        // TODO: 查询所有在途路线的最新位置（每条路线取最新一条）
        throw new UnsupportedOperationException("TODO: implement getLiveLocations");
    }
}
