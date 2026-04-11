package com.smart.dispatch.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.dispatch.dto.DriverRouteOfferDTO;
import com.smart.dispatch.dto.PlanDetailVO;
import com.smart.dispatch.dto.RouteDetailDTO;
import com.smart.dispatch.entity.DispatchPlan;

import java.util.List;

public interface DispatchService {
    Page<DispatchPlan> listPlans(int page, int size);
    PlanDetailVO getPlanDetail(Long planId);
    PlanDetailVO generatePlan(Long creatorId);
    void confirmPlan(Long planId);
    List<DriverRouteOfferDTO> getDriverOffers(Long driverId, String userRole);
    void acceptRoute(Long routeId, Long driverId, String userRole);
    void rejectRoute(Long routeId, Long driverId, String userRole);
    RouteDetailDTO getRouteDetail(Long routeId, Long userId, String userRole);
}
