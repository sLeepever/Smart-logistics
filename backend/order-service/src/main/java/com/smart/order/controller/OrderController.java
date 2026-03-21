package com.smart.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.common.result.Result;
import com.smart.order.entity.Order;
import com.smart.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "订单管理")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "分页查询订单列表")
    @GetMapping
    public Result<Page<Order>> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword) {
        return Result.success(orderService.listOrders(page, size, status, startDate, endDate, keyword));
    }

    @Operation(summary = "查询订单详情")
    @GetMapping("/{id}")
    public Result<Order> getOrder(@PathVariable Long id) {
        return Result.success(orderService.getOrderById(id));
    }

    @Operation(summary = "创建订单")
    @PostMapping
    public Result<Order> createOrder(@RequestBody Order order,
                                     @RequestHeader("X-User-Id") Long userId) {
        return Result.success(orderService.createOrder(order, userId));
    }

    @Operation(summary = "更新订单")
    @PutMapping("/{id}")
    public Result<Order> updateOrder(@PathVariable Long id, @RequestBody Order order) {
        return Result.success(orderService.updateOrder(id, order));
    }

    @Operation(summary = "删除订单")
    @DeleteMapping("/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return Result.success();
    }

    @Operation(summary = "变更订单状态")
    @PatchMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id,
                                     @RequestBody Map<String, String> body) {
        orderService.changeStatus(id, body.get("targetStatus"), body.get("remark"));
        return Result.success();
    }

    @Operation(summary = "变更订单状态（内部调用，供Feign使用）")
    @PostMapping("/{id}/status")
    public Result<Void> changeStatusPost(@PathVariable Long id,
                                         @RequestBody Map<String, String> body) {
        orderService.changeStatus(id, body.get("targetStatus"), body.get("remark"));
        return Result.success();
    }

    @Operation(summary = "获取待调度订单列表（供调度算法使用）")
    @GetMapping("/batch/pending")
    public Result<List<Order>> getPendingOrders() {
        return Result.success(orderService.getPendingOrders());
    }
}
