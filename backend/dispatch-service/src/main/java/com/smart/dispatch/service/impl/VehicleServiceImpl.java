package com.smart.dispatch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.common.exception.BizException;
import com.smart.common.result.ResultCode;
import com.smart.dispatch.entity.Vehicle;
import com.smart.dispatch.mapper.VehicleMapper;
import com.smart.dispatch.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleMapper vehicleMapper;

    @Override
    public Page<Vehicle> listVehicles(int page, int size, String status) {
        LambdaQueryWrapper<Vehicle> wrapper = new LambdaQueryWrapper<Vehicle>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(Vehicle::getStatus, status);
        }
        wrapper.orderByDesc(Vehicle::getCreatedAt);
        return vehicleMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Vehicle getById(Long id) {
        Vehicle v = vehicleMapper.selectById(id);
        if (v == null) {
            throw new BizException(ResultCode.NOT_FOUND, "车辆不存在");
        }
        return v;
    }

    @Override
    public Vehicle create(Vehicle vehicle) {
        vehicle.setDeleted(0);
        vehicle.setCreatedAt(LocalDateTime.now());
        vehicle.setUpdatedAt(LocalDateTime.now());
        vehicleMapper.insert(vehicle);
        return vehicle;
    }

    @Override
    public Vehicle update(Long id, Vehicle vehicle) {
        getById(id);
        vehicle.setId(id);
        vehicle.setUpdatedAt(LocalDateTime.now());
        vehicleMapper.updateById(vehicle);
        return getById(id);
    }

    @Override
    public void delete(Long id) {
        Vehicle v = getById(id);
        if ("on_route".equals(v.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "在途车辆不可删除");
        }
        vehicleMapper.deleteById(id);
    }

    @Override
    public void changeStatus(Long id, String status) {
        getById(id);
        Vehicle update = new Vehicle();
        update.setId(id);
        update.setStatus(status);
        update.setUpdatedAt(LocalDateTime.now());
        vehicleMapper.updateById(update);
    }
}
