package com.smart.tracking.controller;

import com.smart.common.result.Result;
import com.smart.tracking.dto.LocationReportRequest;
import com.smart.tracking.entity.LocationRecord;
import com.smart.tracking.service.TrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "实时跟踪")
@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    @Operation(summary = "司机上报位置")
    @PostMapping("/location")
    public Result<Void> reportLocation(@Valid @RequestBody LocationReportRequest request,
                                       @RequestHeader("X-User-Id") Long driverId) {
        trackingService.reportLocation(request, driverId);
        return Result.success();
    }

    @Operation(summary = "查询路线历史轨迹")
    @GetMapping("/routes/{routeId}/track")
    public Result<List<LocationRecord>> getTrack(@PathVariable Long routeId) {
        return Result.success(trackingService.getTrack(routeId));
    }

    @Operation(summary = "查询所有在途路线最新位置（轮询降级）")
    @GetMapping("/live")
    public Result<List<LocationRecord>> getLiveLocations() {
        return Result.success(trackingService.getLiveLocations());
    }
}
