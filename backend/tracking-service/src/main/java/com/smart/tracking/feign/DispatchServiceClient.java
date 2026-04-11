package com.smart.tracking.feign;

import com.smart.common.result.Result;
import com.smart.tracking.feign.dto.DriverRouteView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "dispatch-service")
public interface DispatchServiceClient {

    @GetMapping("/api/dispatch/driver/routes")
    Result<List<DriverRouteView>> getDriverRoutes(@RequestHeader("X-User-Id") Long driverId,
                                                  @RequestHeader("X-User-Role") String userRole);
}
