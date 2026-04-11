package com.smart.dispatch.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.common.contract.DispatchWorkflowContract;
import com.smart.common.contract.OrderStatusContract;
import com.smart.common.contract.UserRoleContract;
import com.smart.common.exception.BizException;
import com.smart.common.result.ResultCode;
import com.smart.dispatch.algorithm.GeneticOptimizer;
import com.smart.dispatch.algorithm.GeoUtils;
import com.smart.dispatch.algorithm.KMeansClusterer;
import com.smart.dispatch.client.OrderServiceClient;
import com.smart.dispatch.dto.DriverRouteOfferDTO;
import com.smart.dispatch.dto.OrderDTO;
import com.smart.dispatch.dto.PlanDetailVO;
import com.smart.dispatch.dto.RouteDetailDTO;
import com.smart.dispatch.entity.DispatchPlan;
import com.smart.dispatch.entity.Route;
import com.smart.dispatch.entity.RouteOfferCandidate;
import com.smart.dispatch.entity.RouteStop;
import com.smart.dispatch.entity.Vehicle;
import com.smart.dispatch.mapper.DispatchPlanMapper;
import com.smart.dispatch.mapper.RouteMapper;
import com.smart.dispatch.mapper.RouteOfferCandidateMapper;
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
import java.util.Comparator;
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
    private final RouteOfferCandidateMapper routeOfferCandidateMapper;

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
        List<OrderDTO> orders = orderServiceClient.getPendingOrders(UserRoleContract.DISPATCHER).getData();
        if (orders == null || orders.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "没有待调度的订单");
        }
        // 过滤掉坐标无效（0,0）的订单
        orders = orders.stream()
                .filter(o -> Math.abs(o.getReceiverLat()) > 0.0001 && Math.abs(o.getReceiverLng()) > 0.0001)
                .collect(Collectors.toList());
        if (orders.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "所有待调度订单均缺少有效收货坐标，请补充经纬度后重试");
        }
        if (orders.size() > maxOrdersPerBatch) {
            orders = orders.subList(0, maxOrdersPerBatch);
        }

        // 2. 获取空闲车辆
        List<Vehicle> idleVehicles = vehicleMapper.selectList(
                new LambdaQueryWrapper<Vehicle>()
                        .eq(Vehicle::getStatus, DispatchWorkflowContract.VEHICLE_IDLE));
        List<Vehicle> eligibleVehicles = idleVehicles.stream()
                .filter(vehicle -> vehicle.getDriverId() != null)
                .collect(Collectors.toList());
        if (eligibleVehicles.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST, "没有绑定司机的空闲车辆，无法生成方案");
        }

        // 3. 确定 K（聚类数）
        int k = Math.min(eligibleVehicles.size(), Math.max(1, (int) Math.ceil(orders.size() / 5.0)));

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
        List<List<RouteOfferCandidate>> candidatesToSave = new ArrayList<>();
        List<List<RouteStop>> allStops = new ArrayList<>();
        int vehicleIdx = 0;
        int primaryVehicleCount = k;
        List<Vehicle> spareVehicles = primaryVehicleCount < eligibleVehicles.size()
                ? eligibleVehicles.subList(primaryVehicleCount, eligibleVehicles.size())
                : List.of();
        int routeOrdinal = 0;
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
            Vehicle vehicle = eligibleVehicles.get(vehicleIdx++);
            LocalDateTime now = LocalDateTime.now();

            // 构建 Route：offer 阶段尚未写入最终认领司机/车辆
            Route route = new Route();
            route.setVehicleId(null);
            route.setDriverId(null);
            route.setStatus(DispatchWorkflowContract.ROUTE_OFFERED);
            route.setEstimatedDistance(BigDecimal.valueOf(Math.round(afterDist * 10) / 10.0));
            route.setEstimatedDuration((int) (afterDist / AVG_SPEED_KMH * 60));
            route.setDeleted(0);
            route.setCreatedAt(now);
            route.setUpdatedAt(now);
            routesToSave.add(route);

            List<RouteOfferCandidate> routeCandidates = new ArrayList<>();
            routeCandidates.add(buildCandidate(vehicle, DispatchWorkflowContract.ROUTE_OFFERED, now, 1));

            if (routeOrdinal < spareVehicles.size()) {
                Vehicle backupVehicle = spareVehicles.get(routeOrdinal);
                if (!backupVehicle.getId().equals(vehicle.getId()) && backupVehicle.getDriverId() != null) {
                    routeCandidates.add(buildCandidate(backupVehicle, DispatchWorkflowContract.ROUTE_CANDIDATE_QUEUED, null, 2));
                }
            }
            candidatesToSave.add(routeCandidates);
            routeOrdinal++;

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
                ps.setCreatedAt(now);
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
                ds.setCreatedAt(now);
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
        plan.setStatus(DispatchWorkflowContract.PLAN_DRAFT);
        plan.setTotalOrders(orders.size());
        plan.setTotalRoutes(routesToSave.size());
        plan.setBeforeTotalDistance(BigDecimal.valueOf(Math.round(beforeTotal * 10) / 10.0));
        plan.setAfterTotalDistance(BigDecimal.valueOf(Math.round(afterTotal * 10) / 10.0));
        plan.setBeforeVehicleCount(eligibleVehicles.size());
        plan.setAfterVehicleCount(routesToSave.size());
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

            for (RouteOfferCandidate candidate : candidatesToSave.get(i)) {
                candidate.setRouteId(route.getId());
                routeOfferCandidateMapper.insert(candidate);
            }

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
        if (plan == null || !DispatchWorkflowContract.PLAN_DRAFT.equals(plan.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "方案不存在或状态不允许确认");
        }

        List<Route> routes = routeMapper.selectList(
                new LambdaQueryWrapper<Route>().eq(Route::getPlanId, planId));

        for (Route route : routes) {
            if (!isClaimedRoute(route)) {
                continue;
            }
            // 变更订单状态为已调度
            List<RouteStop> deliveryStops = routeStopMapper.selectList(
                    new LambdaQueryWrapper<RouteStop>()
                            .eq(RouteStop::getRouteId, route.getId())
                            .eq(RouteStop::getStopType, "delivery"));
            for (RouteStop stop : deliveryStops) {
                orderServiceClient.changeStatus(stop.getOrderId(),
                        "dispatcher",
                        Map.of("targetStatus", OrderStatusContract.DISPATCHED));
            }

            // 变更车辆状态为在途
            Vehicle vUpdate = new Vehicle();
            vUpdate.setId(route.getVehicleId());
            vUpdate.setStatus(DispatchWorkflowContract.VEHICLE_ON_ROUTE);
            vUpdate.setUpdatedAt(LocalDateTime.now());
            vehicleMapper.updateById(vUpdate);
        }

        // 更新方案状态
        DispatchPlan planUpdate = new DispatchPlan();
        planUpdate.setId(planId);
        planUpdate.setStatus(DispatchWorkflowContract.PLAN_CONFIRMED);
        planUpdate.setConfirmedAt(LocalDateTime.now());
        planUpdate.setUpdatedAt(LocalDateTime.now());
        dispatchPlanMapper.updateById(planUpdate);
    }

    @Override
    public List<DriverRouteOfferDTO> getDriverOffers(Long driverId, String userRole) {
        requireDriverRole(userRole);

        List<RouteOfferCandidate> candidates = routeOfferCandidateMapper.selectList(
                new LambdaQueryWrapper<RouteOfferCandidate>()
                        .eq(RouteOfferCandidate::getDriverId, driverId)
                        .eq(RouteOfferCandidate::getCandidateStatus, DispatchWorkflowContract.ROUTE_OFFERED)
                        .orderByAsc(RouteOfferCandidate::getDisplayOrder)
                        .orderByDesc(RouteOfferCandidate::getOfferedAt));

        return candidates.stream()
                .map(candidate -> toOfferDto(candidate, routeMapper.selectById(candidate.getRouteId())))
                .filter(dto -> dto != null)
                .toList();
    }

    @Override
    @Transactional
    public void acceptRoute(Long routeId, Long driverId, String userRole) {
        requireDriverRole(userRole);

        RouteOfferCandidate candidate = getOfferedCandidate(routeId, driverId);
        Route route = requireRoute(routeId);
        if (!canAccept(route)) {
            throw new BizException(ResultCode.CONFLICT, "路线已被其他司机认领或不再可接受");
        }

        LocalDateTime now = LocalDateTime.now();
        Route routeUpdate = new Route();
        routeUpdate.setStatus(DispatchWorkflowContract.ROUTE_ACCEPTED);
        routeUpdate.setDriverId(candidate.getDriverId());
        routeUpdate.setVehicleId(candidate.getVehicleId());
        routeUpdate.setUpdatedAt(now);

        int updatedRoutes = routeMapper.update(routeUpdate,
                new LambdaUpdateWrapper<Route>()
                        .eq(Route::getId, routeId)
                        .eq(Route::getStatus, DispatchWorkflowContract.ROUTE_OFFERED)
                        .isNull(Route::getDriverId)
                        .isNull(Route::getVehicleId));
        if (updatedRoutes != 1) {
            throw new BizException(ResultCode.CONFLICT, "路线已被其他司机认领");
        }

        RouteOfferCandidate acceptedCandidateUpdate = new RouteOfferCandidate();
        acceptedCandidateUpdate.setCandidateStatus(DispatchWorkflowContract.ROUTE_ACCEPTED);
        acceptedCandidateUpdate.setRespondedAt(now);
        acceptedCandidateUpdate.setUpdatedAt(now);
        int acceptedCandidates = routeOfferCandidateMapper.update(acceptedCandidateUpdate,
                new LambdaUpdateWrapper<RouteOfferCandidate>()
                        .eq(RouteOfferCandidate::getId, candidate.getId())
                        .eq(RouteOfferCandidate::getCandidateStatus, DispatchWorkflowContract.ROUTE_OFFERED));
        if (acceptedCandidates != 1) {
            throw new BizException(ResultCode.CONFLICT, "路线邀约已失效");
        }

        RouteOfferCandidate exhaustedCandidateUpdate = new RouteOfferCandidate();
        exhaustedCandidateUpdate.setCandidateStatus(DispatchWorkflowContract.ROUTE_OFFER_EXHAUSTED);
        exhaustedCandidateUpdate.setRespondedAt(now);
        exhaustedCandidateUpdate.setUpdatedAt(now);
        routeOfferCandidateMapper.update(exhaustedCandidateUpdate,
                new LambdaUpdateWrapper<RouteOfferCandidate>()
                        .eq(RouteOfferCandidate::getRouteId, routeId)
                        .ne(RouteOfferCandidate::getId, candidate.getId())
                        .eq(RouteOfferCandidate::getCandidateStatus, DispatchWorkflowContract.ROUTE_OFFERED));
    }

    @Override
    @Transactional
    public void rejectRoute(Long routeId, Long driverId, String userRole) {
        requireDriverRole(userRole);

        RouteOfferCandidate candidate = getOfferedCandidate(routeId, driverId);
        LocalDateTime now = LocalDateTime.now();

        RouteOfferCandidate rejectedCandidateUpdate = new RouteOfferCandidate();
        rejectedCandidateUpdate.setCandidateStatus(DispatchWorkflowContract.ROUTE_REJECTED);
        rejectedCandidateUpdate.setRespondedAt(now);
        rejectedCandidateUpdate.setUpdatedAt(now);
        int updatedCandidates = routeOfferCandidateMapper.update(rejectedCandidateUpdate,
                new LambdaUpdateWrapper<RouteOfferCandidate>()
                        .eq(RouteOfferCandidate::getId, candidate.getId())
                        .eq(RouteOfferCandidate::getCandidateStatus, DispatchWorkflowContract.ROUTE_OFFERED));
        if (updatedCandidates != 1) {
            throw new BizException(ResultCode.CONFLICT, "路线邀约已失效");
        }

        List<RouteOfferCandidate> remainingOffers = routeOfferCandidateMapper.selectList(
                new LambdaQueryWrapper<RouteOfferCandidate>()
                        .eq(RouteOfferCandidate::getRouteId, routeId)
                        .eq(RouteOfferCandidate::getCandidateStatus, DispatchWorkflowContract.ROUTE_OFFERED));
        if (!remainingOffers.isEmpty()) {
            return;
        }

        RouteOfferCandidate nextCandidate = routeOfferCandidateMapper.selectOne(
                new LambdaQueryWrapper<RouteOfferCandidate>()
                        .eq(RouteOfferCandidate::getRouteId, routeId)
                        .eq(RouteOfferCandidate::getCandidateStatus, DispatchWorkflowContract.ROUTE_CANDIDATE_QUEUED)
                        .orderByAsc(RouteOfferCandidate::getDisplayOrder)
                        .last("LIMIT 1"));
        if (nextCandidate != null) {
            RouteOfferCandidate nextCandidateUpdate = new RouteOfferCandidate();
            nextCandidateUpdate.setCandidateStatus(DispatchWorkflowContract.ROUTE_OFFERED);
            nextCandidateUpdate.setOfferedAt(now);
            nextCandidateUpdate.setUpdatedAt(now);
            routeOfferCandidateMapper.update(nextCandidateUpdate,
                    new LambdaUpdateWrapper<RouteOfferCandidate>()
                            .eq(RouteOfferCandidate::getId, nextCandidate.getId())
                            .eq(RouteOfferCandidate::getCandidateStatus, DispatchWorkflowContract.ROUTE_CANDIDATE_QUEUED));
            return;
        }

        Route routeUpdate = new Route();
        routeUpdate.setStatus(DispatchWorkflowContract.ROUTE_OFFER_EXHAUSTED);
        routeUpdate.setDriverId(null);
        routeUpdate.setVehicleId(null);
        routeUpdate.setUpdatedAt(now);
        routeMapper.update(routeUpdate,
                new LambdaUpdateWrapper<Route>()
                        .eq(Route::getId, routeId)
                        .eq(Route::getStatus, DispatchWorkflowContract.ROUTE_OFFERED)
                        .isNull(Route::getDriverId)
                        .isNull(Route::getVehicleId));
    }

    @Override
    public RouteDetailDTO getRouteDetail(Long routeId, Long userId, String userRole) {
        Route route = requireRoute(routeId);

        if (UserRoleContract.isOperational(userRole)) {
            return buildRouteDetail(route, true);
        }
        if (!UserRoleContract.DRIVER.equals(userRole)) {
            throw new BizException(ResultCode.FORBIDDEN, "当前角色无权查看路线详情");
        }
        if (isAssignedDriver(route, userId)) {
            return buildRouteDetail(route, true);
        }

        RouteOfferCandidate candidate = routeOfferCandidateMapper.selectOne(
                new LambdaQueryWrapper<RouteOfferCandidate>()
                        .eq(RouteOfferCandidate::getRouteId, routeId)
                        .eq(RouteOfferCandidate::getDriverId, userId)
                        .eq(RouteOfferCandidate::getCandidateStatus, DispatchWorkflowContract.ROUTE_OFFERED)
                        .last("LIMIT 1"));
        if (candidate != null) {
            return buildRouteDetail(route, false);
        }

        throw new BizException(ResultCode.FORBIDDEN, "无权查看该路线详情");
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

    private boolean isClaimedRoute(Route route) {
        return DispatchWorkflowContract.ROUTE_ACCEPTED.equals(route.getStatus())
                && route.getVehicleId() != null
                && route.getDriverId() != null;
    }

    private DriverRouteOfferDTO toOfferDto(RouteOfferCandidate candidate, Route route) {
        if (route == null || !DispatchWorkflowContract.ROUTE_OFFERED.equals(route.getStatus())) {
            return null;
        }

        DriverRouteOfferDTO dto = new DriverRouteOfferDTO();
        dto.setRouteId(route.getId());
        dto.setPlanId(route.getPlanId());
        dto.setVehicleId(candidate.getVehicleId());
        dto.setRouteStatus(route.getStatus());
        dto.setCandidateStatus(candidate.getCandidateStatus());
        dto.setEstimatedDistance(route.getEstimatedDistance());
        dto.setEstimatedDuration(route.getEstimatedDuration());
        dto.setDisplayOrder(candidate.getDisplayOrder());
        dto.setOfferedAt(candidate.getOfferedAt());
        dto.setDetailsVisible(false);
        return dto;
    }

    private RouteOfferCandidate getOfferedCandidate(Long routeId, Long driverId) {
        RouteOfferCandidate candidate = routeOfferCandidateMapper.selectOne(
                new LambdaQueryWrapper<RouteOfferCandidate>()
                        .eq(RouteOfferCandidate::getRouteId, routeId)
                        .eq(RouteOfferCandidate::getDriverId, driverId)
                        .eq(RouteOfferCandidate::getCandidateStatus, DispatchWorkflowContract.ROUTE_OFFERED)
                        .last("LIMIT 1"));
        if (candidate == null) {
            throw new BizException(ResultCode.CONFLICT, "路线邀约不存在或已处理");
        }
        return candidate;
    }

    private Route requireRoute(Long routeId) {
        Route route = routeMapper.selectById(routeId);
        if (route == null) {
            throw new BizException(ResultCode.ROUTE_NOT_FOUND);
        }
        return route;
    }

    private boolean canAccept(Route route) {
        return DispatchWorkflowContract.ROUTE_OFFERED.equals(route.getStatus())
                && route.getDriverId() == null
                && route.getVehicleId() == null;
    }

    private boolean isAssignedDriver(Route route, Long userId) {
        return route.getDriverId() != null
                && route.getDriverId().equals(userId)
                && (DispatchWorkflowContract.ROUTE_ACCEPTED.equals(route.getStatus())
                || DispatchWorkflowContract.ROUTE_IN_PROGRESS.equals(route.getStatus())
                || DispatchWorkflowContract.ROUTE_COMPLETED.equals(route.getStatus()));
    }

    private RouteDetailDTO buildRouteDetail(Route route, boolean detailsVisible) {
        RouteDetailDTO dto = new RouteDetailDTO();
        dto.setRouteId(route.getId());
        dto.setPlanId(route.getPlanId());
        dto.setVehicleId(route.getVehicleId());
        dto.setDriverId(route.getDriverId());
        dto.setStatus(route.getStatus());
        dto.setEstimatedDistance(route.getEstimatedDistance());
        dto.setEstimatedDuration(route.getEstimatedDuration());
        dto.setDetailsVisible(detailsVisible);
        if (detailsVisible) {
            dto.setStops(routeStopMapper.selectList(
                            new LambdaQueryWrapper<RouteStop>()
                                    .eq(RouteStop::getRouteId, route.getId())
                                    .orderByAsc(RouteStop::getStopSeq))
                    .stream()
                    .sorted(Comparator.comparing(RouteStop::getStopSeq))
                    .map(this::toRouteStopDto)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    private RouteDetailDTO.RouteStopDTO toRouteStopDto(RouteStop stop) {
        RouteDetailDTO.RouteStopDTO dto = new RouteDetailDTO.RouteStopDTO();
        dto.setId(stop.getId());
        dto.setOrderId(stop.getOrderId());
        dto.setStopSeq(stop.getStopSeq());
        dto.setStopType(stop.getStopType());
        dto.setAddress(stop.getAddress());
        dto.setLng(stop.getLng());
        dto.setLat(stop.getLat());
        dto.setArrivedAt(stop.getArrivedAt());
        return dto;
    }

    private void requireDriverRole(String userRole) {
        if (!UserRoleContract.DRIVER.equals(userRole)) {
            throw new BizException(ResultCode.FORBIDDEN, "当前角色无权执行司机邀约操作");
        }
    }

    private RouteOfferCandidate buildCandidate(Vehicle vehicle, String candidateStatus, LocalDateTime offeredAt, int displayOrder) {
        LocalDateTime now = LocalDateTime.now();
        RouteOfferCandidate candidate = new RouteOfferCandidate();
        candidate.setVehicleId(vehicle.getId());
        candidate.setDriverId(vehicle.getDriverId());
        candidate.setCandidateStatus(candidateStatus);
        candidate.setOfferedAt(offeredAt);
        candidate.setDisplayOrder(displayOrder);
        candidate.setCreatedAt(now);
        candidate.setUpdatedAt(now);
        return candidate;
    }
}
