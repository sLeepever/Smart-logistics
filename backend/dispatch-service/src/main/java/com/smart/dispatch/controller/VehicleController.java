package com.smart.dispatch.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.common.result.Result;
import com.smart.dispatch.entity.Vehicle;
import com.smart.dispatch.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "车辆管理")
@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @Operation(summary = "分页查询车辆列表")
    @GetMapping
    public Result<Page<Vehicle>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return Result.success(vehicleService.listVehicles(page, size, status));
    }

    @Operation(summary = "查询车辆详情")
    @GetMapping("/{id}")
    public Result<Vehicle> getById(@PathVariable Long id) {
        return Result.success(vehicleService.getById(id));
    }

    @Operation(summary = "新建车辆")
    @PostMapping
    public Result<Vehicle> create(@RequestBody Vehicle vehicle) {
        return Result.success(vehicleService.create(vehicle));
    }

    @Operation(summary = "更新车辆信息")
    @PutMapping("/{id}")
    public Result<Vehicle> update(@PathVariable Long id, @RequestBody Vehicle vehicle) {
        return Result.success(vehicleService.update(id, vehicle));
    }

    @Operation(summary = "删除车辆")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        vehicleService.delete(id);
        return Result.success();
    }

    @Operation(summary = "变更车辆状态")
    @PatchMapping("/{id}/status")
    public Result<Void> changeStatus(@PathVariable Long id,
                                     @RequestBody Map<String, String> body) {
        vehicleService.changeStatus(id, body.get("status"));
        return Result.success();
    }
}
