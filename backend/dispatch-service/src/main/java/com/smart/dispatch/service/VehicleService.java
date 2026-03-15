package com.smart.dispatch.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.dispatch.entity.Vehicle;

public interface VehicleService {
    Page<Vehicle> listVehicles(int page, int size, String status);
    Vehicle getById(Long id);
    Vehicle create(Vehicle vehicle);
    Vehicle update(Long id, Vehicle vehicle);
    void delete(Long id);
    void changeStatus(Long id, String status);
}
