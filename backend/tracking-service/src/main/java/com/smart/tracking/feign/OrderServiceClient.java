package com.smart.tracking.feign;

import com.smart.common.result.Result;
import com.smart.tracking.feign.dto.OrderSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/api/orders/{id}")
    Result<OrderSummary> getOrder(@PathVariable("id") Long orderId,
                                  @RequestHeader("X-User-Id") Long userId,
                                  @RequestHeader("X-User-Role") String userRole);
}
