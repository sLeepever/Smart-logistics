package com.smart.order.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.common.contract.UserRoleContract;
import com.smart.common.exception.BizException;
import com.smart.common.result.Result;
import com.smart.common.result.ResultCode;
import com.smart.order.dto.OrderReviewRequest;
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
            @RequestParam(required = false) String keyword,
            @RequestHeader("X-User-Role") String userRole) {
        return Result.success(orderService.listOrders(page, size, status, startDate, endDate, keyword, userRole));
    }

    @Operation(summary = "分页查询我的订单列表")
    @GetMapping("/mine")
    public Result<Page<Order>> listMyOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String keyword,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole) {
        return Result.success(orderService.listMyOrders(page, size, status, startDate, endDate, keyword, userId, userRole));
    }

    @Operation(summary = "查询订单详情")
    @GetMapping("/{id}")
    public Result<Order> getOrder(@PathVariable Long id,
                                  @RequestHeader("X-User-Id") Long userId,
                                  @RequestHeader("X-User-Role") String userRole) {
        return Result.success(orderService.getOrderById(id, userId, userRole));
    }

    @Operation(summary = "创建订单")
    @PostMapping
    public Result<Order> createOrder(@RequestBody Order order,
                                     @RequestHeader("X-User-Id") Long userId,
                                     @RequestHeader("X-User-Role") String userRole) {
        return Result.success(orderService.createOrder(order, userId, userRole));
    }

    @Operation(summary = "更新订单")
    @PutMapping("/{id}")
    public Result<Order> updateOrder(@PathVariable Long id,
                                     @RequestBody Order order,
                                     @RequestHeader("X-User-Id") Long userId,
                                     @RequestHeader("X-User-Role") String userRole) {
        return Result.success(orderService.updateOrder(id, order, userId, userRole));
    }

    @Operation(summary = "删除订单")
    @DeleteMapping("/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id,
                                    @RequestHeader("X-User-Id") Long userId,
                                    @RequestHeader("X-User-Role") String userRole) {
        orderService.deleteOrder(id, userId, userRole);
        return Result.success();
    }

    @Operation(summary = "变更订单状态")
    @PatchMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id,
                                     @RequestBody Map<String, String> body,
                                     @RequestHeader("X-User-Id") Long userId,
                                     @RequestHeader("X-User-Role") String userRole) {
        orderService.changeStatus(id, body.get("targetStatus"), body.get("remark"), userId, userRole);
        return Result.success();
    }

    @Operation(summary = "变更订单状态（内部调用，供Feign使用）")
    @PostMapping("/{id}/status")
    public Result<Void> changeStatusPost(@PathVariable Long id,
                                         @RequestBody Map<String, String> body,
                                         @RequestHeader("X-User-Role") String userRole) {
        orderService.changeStatus(id, body.get("targetStatus"), body.get("remark"), null, userRole);
        return Result.success();
    }

    @Operation(summary = "取消待审核订单")
    @PatchMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id,
                                    @RequestHeader("X-User-Id") Long userId,
                                    @RequestHeader("X-User-Role") String userRole) {
        orderService.cancelOrder(id, userId, userRole);
        return Result.success();
    }

    @Operation(summary = "审核待审核订单")
    @PatchMapping("/{id}/review")
    public Result<Void> reviewOrder(@PathVariable Long id,
                                    @RequestBody OrderReviewRequest request,
                                    @RequestHeader("X-User-Id") Long userId,
                                    @RequestHeader("X-User-Role") String userRole) {
        orderService.reviewOrder(id, request.getAction(), request.getRemark(), userId, userRole);
        return Result.success();
    }

    @Operation(summary = "获取待调度订单列表（供调度算法使用）")
    @GetMapping("/batch/pending")
    public Result<List<Order>> getPendingOrders(@RequestHeader("X-User-Role") String userRole) {
        requireOperationalRole(userRole);
        return Result.success(orderService.getPendingOrders());
    }

    private void requireOperationalRole(String userRole) {
        if (!UserRoleContract.isOperational(userRole)) {
            throw new BizException(ResultCode.FORBIDDEN, "仅管理员或调度员可访问待调度订单列表");
        }
    }
}
