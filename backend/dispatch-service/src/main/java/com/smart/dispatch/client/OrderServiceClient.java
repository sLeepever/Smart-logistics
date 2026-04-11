package com.smart.dispatch.client;

import com.smart.common.result.Result;
import com.smart.dispatch.dto.OrderDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@FeignClient(name = "order-service")
public interface OrderServiceClient {

    @GetMapping("/api/orders/batch/pending")
    Result<List<OrderDTO>> getPendingOrders(@RequestHeader("X-User-Role") String userRole);

    @PostMapping("/api/orders/{id}/status")
    Result<Void> changeStatus(@PathVariable("id") Long id,
                              @RequestHeader("X-User-Role") String userRole,
                              @RequestBody Map<String, String> body);
}
