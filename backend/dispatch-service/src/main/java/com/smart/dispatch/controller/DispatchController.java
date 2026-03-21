package com.smart.dispatch.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.common.result.Result;
import com.smart.dispatch.dto.PlanDetailVO;
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
            @RequestHeader("X-User-Id") Long driverId) {
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

    @Operation(summary = "分页查询调度方案列表")
    @GetMapping("/plans")
    public Result<Page<DispatchPlan>> listPlans(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(dispatchService.listPlans(page, size));
    }

    @Operation(summary = "查询调度方案详情")
    @GetMapping("/plans/{id}")
    public Result<PlanDetailVO> getPlanDetail(@PathVariable Long id) {
        return Result.success(dispatchService.getPlanDetail(id));
    }

    @Operation(summary = "生成调度方案（运行算法）")
    @PostMapping("/plans/generate")
    public Result<PlanDetailVO> generatePlan(
            @RequestHeader("X-User-Id") Long userId) {
        return Result.success(dispatchService.generatePlan(userId));
    }

    @Operation(summary = "确认调度方案（变更订单状态为已调度）")
    @PostMapping("/plans/{id}/confirm")
    public Result<Void> confirmPlan(@PathVariable Long id) {
        dispatchService.confirmPlan(id);
        return Result.success();
    }
}
