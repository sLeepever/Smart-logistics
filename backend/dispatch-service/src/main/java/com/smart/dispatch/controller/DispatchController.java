package com.smart.dispatch.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.common.result.Result;
import com.smart.dispatch.dto.PlanDetailVO;
import com.smart.dispatch.entity.DispatchPlan;
import com.smart.dispatch.service.DispatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "调度管理")
@RestController
@RequestMapping("/api/dispatch")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchService dispatchService;

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
