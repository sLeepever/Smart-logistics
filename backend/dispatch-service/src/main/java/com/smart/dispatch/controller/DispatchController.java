package com.smart.dispatch.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.common.contract.UserRoleContract;
import com.smart.common.exception.BizException;
import com.smart.common.result.Result;
import com.smart.common.result.ResultCode;
import com.smart.dispatch.dto.DriverRouteOfferDTO;
import com.smart.dispatch.dto.PlanDetailVO;
import com.smart.dispatch.dto.RouteDetailDTO;
import com.smart.dispatch.entity.DispatchPlan;
import com.smart.dispatch.entity.Route;
import com.smart.dispatch.entity.RouteStop;
import com.smart.dispatch.mapper.RouteMapper;
import com.smart.dispatch.mapper.RouteStopMapper;
import com.smart.dispatch.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "调度管理")
@RestController
@RequestMapping("/api/dispatch")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchService dispatchService;
    private final RouteMapper routeMapper;
    private final RouteStopMapper routeStopMapper;

    @Operation(summary = "司机查询自己的路线任务")
    @GetMapping("/driver/routes")
    public Result<List<Map<String, Object>>> getDriverRoutes(
            @RequestHeader("X-User-Id") Long driverId,
            @RequestHeader("X-User-Role") String userRole) {
        requireDriverRole(userRole);
        List<Route> routes = routeMapper.selectList(
                new LambdaQueryWrapper<Route>().eq(Route::getDriverId, driverId));
        List<Map<String, Object>> result = routes.stream().map(route -> {
            List<RouteStop> stops = routeStopMapper.selectList(
                    new LambdaQueryWrapper<RouteStop>()
                            .eq(RouteStop::getRouteId, route.getId())
                            .orderByAsc(RouteStop::getStopSeq));
            Map<String, Object> item = new HashMap<>();
            item.put("route", route);
            item.put("stops", stops);
            return item;
        }).toList();
        return Result.success(result);
    }

    @Operation(summary = "司机查询待响应的路线邀约")
    @GetMapping("/driver/offers")
    public Result<List<DriverRouteOfferDTO>> getDriverOffers(
            @RequestHeader("X-User-Id") Long driverId,
            @RequestHeader("X-User-Role") String userRole) {
        return Result.success(dispatchService.getDriverOffers(driverId, userRole));
    }

    @Operation(summary = "司机接受路线邀约")
    @PostMapping("/routes/{routeId}/accept")
    public Result<Void> acceptRoute(
            @PathVariable Long routeId,
            @RequestHeader("X-User-Id") Long driverId,
            @RequestHeader("X-User-Role") String userRole) {
        dispatchService.acceptRoute(routeId, driverId, userRole);
        return Result.success();
    }

    @Operation(summary = "司机拒绝路线邀约")
    @PostMapping("/routes/{routeId}/reject")
    public Result<Void> rejectRoute(
            @PathVariable Long routeId,
            @RequestHeader("X-User-Id") Long driverId,
            @RequestHeader("X-User-Role") String userRole) {
        dispatchService.rejectRoute(routeId, driverId, userRole);
        return Result.success();
    }

    @Operation(summary = "按角色返回路线详情")
    @GetMapping("/routes/{routeId}")
    public Result<RouteDetailDTO> getRouteDetail(
            @PathVariable Long routeId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole) {
        return Result.success(dispatchService.getRouteDetail(routeId, userId, userRole));
    }

    @Operation(summary = "分页查询调度方案列表")
    @GetMapping("/plans")
    public Result<Page<DispatchPlan>> listPlans(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("X-User-Role") String userRole) {
        requireOperationalRole(userRole);
        return Result.success(dispatchService.listPlans(page, size));
    }

    @Operation(summary = "查询调度方案详情")
    @GetMapping("/plans/{id}")
    public Result<PlanDetailVO> getPlanDetail(@PathVariable Long id,
                                              @RequestHeader("X-User-Role") String userRole) {
        requireOperationalRole(userRole);
        return Result.success(dispatchService.getPlanDetail(id));
    }

    @Operation(summary = "生成调度方案（运行算法）")
    @PostMapping("/plans/generate")
    public Result<PlanDetailVO> generatePlan(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole) {
        requireOperationalRole(userRole);
        return Result.success(dispatchService.generatePlan(userId));
    }

    @Operation(summary = "确认调度方案（变更订单状态为已调度）")
    @PostMapping("/plans/{id}/confirm")
    public Result<Void> confirmPlan(@PathVariable Long id,
                                    @RequestHeader("X-User-Role") String userRole) {
        requireOperationalRole(userRole);
        dispatchService.confirmPlan(id);
        return Result.success();
    }

    private void requireDriverRole(String userRole) {
        if (!UserRoleContract.DRIVER.equals(userRole)) {
            throw new BizException(ResultCode.FORBIDDEN, "仅司机可访问司机路线接口");
        }
    }

    private void requireOperationalRole(String userRole) {
        if (!UserRoleContract.isOperational(userRole)) {
            throw new BizException(ResultCode.FORBIDDEN, "仅管理员或调度员可访问调度方案接口");
        }
    }
}
