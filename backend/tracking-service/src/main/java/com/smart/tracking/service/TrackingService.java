package com.smart.tracking.service;

import com.smart.tracking.dto.LocationReportRequest;
import com.smart.tracking.entity.LocationRecord;

import java.util.List;

public interface TrackingService {
    void reportLocation(LocationReportRequest request, Long driverId);
    List<LocationRecord> getTrack(Long routeId);
    List<LocationRecord> getLiveLocations();
}
