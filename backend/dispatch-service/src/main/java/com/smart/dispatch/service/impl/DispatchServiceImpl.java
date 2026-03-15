package com.smart.dispatch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.common.exception.BizException;
import com.smart.common.result.ResultCode;
import com.smart.dispatch.algorithm.GeneticOptimizer;
import com.smart.dispatch.algorithm.GeoUtils;
import com.smart.dispatch.algorithm.KMeansClusterer;
import com.smart.dispatch.client.OrderServiceClient;
import com.smart.dispatch.dto.OrderDTO;
import com.smart.dispatch.dto.PlanDetailVO;
import com.smart.dispatch.entity.DispatchPlan;
import com.smart.dispatch.entity.Route;
import com.smart.dispatch.entity.RouteStop;
import com.smart.dispatch.entity.Vehicle;
import com.smart.dispatch.mapper.DispatchPlanMapper;
import com.smart.dispatch.mapper.RouteMapper;
import com.smart.dispatch.mapper.RouteStopMapper;
import com.smart.dispatch.mapper.VehicleMapper;
import com.smart.dispatch.service.DispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DispatchServiceImpl implements DispatchService {

    private static final double DEPOT_LAT = 23.0452;
    private static final double DEPOT_LNG = 113.3960;
    private static final String DEPOT_ADDRESS = "广东工业大学大学城校区仓库";
    // 平均车速 40km/h，估算行驶时长（分钟）
    private static final double AVG_SPEED_KMH = 40.0;

    @Value("${dispatch.algorithm.ga-population-size:50}")
    private int gaPopSize;
    @Value("${dispatch.algorithm.ga-max-iterations:200}")
    private int gaMaxIter;
    @Value("${dispatch.algorithm.ga-mutation-rate:0.05}")
    private double gaMutationRate;
    @Value("${dispatch.algorithm.max-orders-per-batch:50}")
    private int maxOrdersPerBatch;

    private final OrderServiceClient orderServiceClient;
    private final VehicleMapper vehicleMapper;
    private final DispatchPlanMapper dispatchPlanMapper;
    private final RouteMapper routeMapper;
    private final RouteStopMapper routeStopMapper;

    @Override
    public Page<DispatchPlan> listPlans(int page, int size) {
        return dispatchPlanMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<DispatchPlan>()
                        .orderByDesc(DispatchPlan::getCreatedAt));
    }

    @Override
    public PlanDetailVO getPlanDetail(Long planId) {
        DispatchPlan plan = dispatchPlanMapper.selectById(planId);
        if (plan == null) throw new BizException(ResultCode.NOT_FOUND, "方案不存在");

        List<Route> routes = routeMapper.selectList(
                new LambdaQueryWrapper<Route>().eq(Route::getPlanId, planId));

        List<PlanDetailVO.RouteVO> routeVOs = new ArrayList<>();
        for (Route route : routes) {
            List<RouteStop> stops = routeStopMapper.selectList(
                    new LambdaQueryWrapper<RouteStop>()
                            .eq(RouteStop::getRouteId, route.getId())
                            .orderByAsc(RouteStop::getStopSeq));
            PlanDetailVO.RouteVO vo = new PlanDetailVO.RouteVO();
            vo.setRoute(route);
            vo.setStops(stops);
            routeVOs.add(vo);
        }

        PlanDetailVO detail = new PlanDetailVO();
        detail.setPlan(plan);
        detail.setRoutes(routeVOs);
        return detail;
    }

    @Override
    @Transactional
    public PlanDetailVO generatePlan(Long creatorId) {
        // 1. 获取待调度订单
        List<OrderDTO> orders = orderServiceClient.getPendingOrders().getData();
        if (orders == null || orders.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "没有待调度的订单");
        }
        if (orders.size() > maxOrdersPerBatch) {
            orders = orders.subList(0, maxOrdersPerBatch);
        }

        // 2. 获取空闲车辆
        List<Vehicle> idleVehicles = vehicleMapper.selectList(
                new LambdaQueryWrapper<Vehicle>()
                        .eq(Vehicle::getStatus, "idle"));
        if (idleVehicles.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "没有空闲车辆，无法生成方案");
        }

        // 3. 确定 K（聚类数）
        int k = Math.min(idleVehicles.size(), Math.max(1, (int) Math.ceil(orders.size() / 5.0)));

        // 4. K-Means 聚类
        List<double[]> points = orders.stream()
                .map(o -> new double[]{o.getReceiverLat(), o.getReceiverLng()})
                .collect(Collectors.toList());
        Map<Integer, List<Integer>> clusters = KMeansClusterer.cluster(points, k);

        // 5. 对各簇运行 GA 优化，收集路线数据
        GeneticOptimizer ga = new GeneticOptimizer(gaPopSize, gaMaxIter, gaMutationRate);
        double beforeTotal = 0;
        double afterTotal = 0;

        List<Route> routesToSave = new ArrayList<>();
        List<List<RouteStop>> allStops = new ArrayList<>();
        List<Long> assignedVehicleIds = new ArrayList<>();

        int vehicleIdx = 0;
        for (Map.Entry<Integer, List<Integer>> entry : clusters.entrySet()) {
            List<Integer> orderIndices = entry.getValue();
            List<OrderDTO> clusterOrders = orderIndices.stream()
                    .map(orders::get)
                    .collect(Collectors.toList());

            double[][] pts = new double[clusterOrders.size()][2];
            for (int i = 0; i < clusterOrders.size(); i++) {
                pts[i][0] = clusterOrders.get(i).getReceiverLat();
                pts[i][1] = clusterOrders.get(i).getReceiverLng();
            }

            // before: 顺序路线距离
            beforeTotal += GeoUtils.sequentialDistance(pts, DEPOT_LAT, DEPOT_LNG);

            // after: GA 优化后距离
            int[] optimized = ga.optimize(pts, DEPOT_LAT, DEPOT_LNG);
            double afterDist = GeoUtils.routeDistance(optimized, pts, DEPOT_LAT, DEPOT_LNG);
            afterTotal += afterDist;

            // 分配车辆
            Vehicle vehicle = idleVehicles.get(vehicleIdx++);
            assignedVehicleIds.add(vehicle.getId());

            // 构建 Route
            Route route = new Route();
            route.setVehicleId(vehicle.getId());
            route.setDriverId(vehicle.getDriverId());
            route.setStatus("assigned");
            route.setEstimatedDistance(BigDecimal.valueOf(Math.round(afterDist * 10) / 10.0));
            route.setEstimatedDuration((int) (afterDist / AVG_SPEED_KMH * 60));
            route.setDeleted(0);
            route.setCreatedAt(LocalDateTime.now());
            route.setUpdatedAt(LocalDateTime.now());
            routesToSave.add(route);

            // 构建 RouteStop（先取货后送货）
            List<RouteStop> stops = new ArrayList<>();
            for (int i = 0; i < clusterOrders.size(); i++) {
                RouteStop ps = new RouteStop();
                ps.setOrderId(clusterOrders.get(i).getId());
                ps.setStopSeq(i + 1);
                ps.setStopType("pickup");
                ps.setAddress(DEPOT_ADDRESS);
                ps.setLng(BigDecimal.valueOf(DEPOT_LNG));
                ps.setLat(BigDecimal.valueOf(DEPOT_LAT));
                ps.setCreatedAt(LocalDateTime.now());
                stops.add(ps);
            }
            for (int i = 0; i < optimized.length; i++) {
                OrderDTO o = clusterOrders.get(optimized[i]);
                RouteStop ds = new RouteStop();
                ds.setOrderId(o.getId());
                ds.setStopSeq(clusterOrders.size() + i + 1);
                ds.setStopType("delivery");
                ds.setAddress(o.getReceiverAddress());
                ds.setLng(BigDecimal.valueOf(o.getReceiverLng()));
                ds.setLat(BigDecimal.valueOf(o.getReceiverLat()));
                ds.setCreatedAt(LocalDateTime.now());
                stops.add(ds);
            }
            allStops.add(stops);
        }

        // 6. 保存调度方案
        String algoParams = String.format(
                "{\"popSize\":%d,\"maxIter\":%d,\"mutationRate\":%.2f,\"clusterK\":%d}",
                gaPopSize, gaMaxIter, gaMutationRate, k);

        DispatchPlan plan = new DispatchPlan();
        plan.setPlanNo(generatePlanNo());
        plan.setStatus("draft");
        plan.setTotalOrders(orders.size());
        plan.setTotalRoutes(routesToSave.size());
        plan.setBeforeTotalDistance(BigDecimal.valueOf(Math.round(beforeTotal * 10) / 10.0));
        plan.setAfterTotalDistance(BigDecimal.valueOf(Math.round(afterTotal * 10) / 10.0));
        plan.setBeforeVehicleCount(idleVehicles.size());
        plan.setAfterVehicleCount(k);
        plan.setAlgorithmParams(algoParams);
        plan.setCreatedBy(creatorId);
        plan.setDeleted(0);
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        dispatchPlanMapper.insert(plan);

        // 7. 保存路线和途经点
        for (int i = 0; i < routesToSave.size(); i++) {
            Route route = routesToSave.get(i);
            route.setPlanId(plan.getId());
            routeMapper.insert(route);
            for (RouteStop stop : allStops.get(i)) {
                stop.setRouteId(route.getId());
                routeStopMapper.insert(stop);
            }
        }

        return getPlanDetail(plan.getId());
    }

    @Override
    @Transactional
    public void confirmPlan(Long planId) {
        DispatchPlan plan = dispatchPlanMapper.selectById(planId);
        if (plan == null || !"draft".equals(plan.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "方案不存在或状态不允许确认");
        }

        List<Route> routes = routeMapper.selectList(
                new LambdaQueryWrapper<Route>().eq(Route::getPlanId, planId));

        for (Route route : routes) {
            // 变更订单状态为已调度
            List<RouteStop> deliveryStops = routeStopMapper.selectList(
                    new LambdaQueryWrapper<RouteStop>()
                            .eq(RouteStop::getRouteId, route.getId())
                            .eq(RouteStop::getStopType, "delivery"));
            for (RouteStop stop : deliveryStops) {
                orderServiceClient.changeStatus(stop.getOrderId(),
                        Map.of("targetStatus", "dispatched"));
            }

            // 变更车辆状态为在途
            if (route.getVehicleId() != null) {
                Vehicle vUpdate = new Vehicle();
                vUpdate.setId(route.getVehicleId());
                vUpdate.setStatus("on_route");
                vUpdate.setUpdatedAt(LocalDateTime.now());
                vehicleMapper.updateById(vUpdate);
            }
        }

        // 更新方案状态
        DispatchPlan planUpdate = new DispatchPlan();
        planUpdate.setId(planId);
        planUpdate.setStatus("confirmed");
        planUpdate.setConfirmedAt(LocalDateTime.now());
        planUpdate.setUpdatedAt(LocalDateTime.now());
        dispatchPlanMapper.updateById(planUpdate);
    }

    private String generatePlanNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PLN" + date;
        DispatchPlan last = dispatchPlanMapper.selectOne(
                new LambdaQueryWrapper<DispatchPlan>()
                        .likeRight(DispatchPlan::getPlanNo, prefix)
                        .orderByDesc(DispatchPlan::getPlanNo)
                        .last("LIMIT 1"));
        int seq = last == null ? 1
                : Integer.parseInt(last.getPlanNo().substring(prefix.length())) + 1;
        return prefix + String.format("%03d", seq);
    }
}
