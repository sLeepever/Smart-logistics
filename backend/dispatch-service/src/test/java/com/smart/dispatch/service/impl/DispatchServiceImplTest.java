package com.smart.dispatch.service.impl;

import com.smart.common.contract.DispatchWorkflowContract;
import com.smart.common.contract.UserRoleContract;
import com.smart.common.exception.BizException;
import com.smart.common.result.Result;
import com.smart.common.result.ResultCode;
import com.smart.dispatch.dto.RouteDetailDTO;
import com.smart.dispatch.client.OrderServiceClient;
import com.smart.dispatch.dto.OrderDTO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchServiceImplTest {

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private VehicleMapper vehicleMapper;

    @Mock
    private DispatchPlanMapper dispatchPlanMapper;

    @Mock
    private RouteMapper routeMapper;

    @Mock
    private RouteStopMapper routeStopMapper;

    @Mock
    private RouteOfferCandidateMapper routeOfferCandidateMapper;

    private DispatchServiceImpl dispatchService;

    @BeforeEach
    void setUp() {
        dispatchService = new DispatchServiceImpl(
                orderServiceClient,
                vehicleMapper,
                dispatchPlanMapper,
                routeMapper,
                routeStopMapper,
                routeOfferCandidateMapper);
        ReflectionTestUtils.setField(dispatchService, "gaPopSize", 20);
        ReflectionTestUtils.setField(dispatchService, "gaMaxIter", 30);
        ReflectionTestUtils.setField(dispatchService, "gaMutationRate", 0.05d);
        ReflectionTestUtils.setField(dispatchService, "maxOrdersPerBatch", 50);
    }

    @Test
    void generatePlanInsertsOfferedRoutesWithNullAssignees() {
        GeneratePlanFixture fixture = stubGeneratePlanPersistence();

        dispatchService.generatePlan(900L);

        ArgumentCaptor<Route> routeCaptor = ArgumentCaptor.forClass(Route.class);
        verify(routeMapper).insert(routeCaptor.capture());

        Route route = routeCaptor.getValue();
        assertEquals(DispatchWorkflowContract.ROUTE_OFFERED, route.getStatus());
        assertNull(route.getVehicleId());
        assertNull(route.getDriverId());
        assertEquals(fixture.planId(), route.getPlanId());
    }

    @Test
    void generatePlanCreatesCandidateRowsFromEligibleIdleVehicles() {
        GeneratePlanFixture fixture = stubGeneratePlanPersistence();

        dispatchService.generatePlan(901L);

        ArgumentCaptor<RouteOfferCandidate> candidateCaptor = ArgumentCaptor.forClass(RouteOfferCandidate.class);
        verify(routeOfferCandidateMapper, times(2)).insert(candidateCaptor.capture());

        List<RouteOfferCandidate> candidates = candidateCaptor.getAllValues();
        RouteOfferCandidate primaryCandidate = candidates.get(0);
        RouteOfferCandidate queuedCandidate = candidates.get(1);

        assertEquals(11L, primaryCandidate.getVehicleId());
        assertEquals(101L, primaryCandidate.getDriverId());
        assertEquals(DispatchWorkflowContract.ROUTE_OFFERED, primaryCandidate.getCandidateStatus());
        assertEquals(1, primaryCandidate.getDisplayOrder());
        assertNotNull(primaryCandidate.getOfferedAt());
        assertNotNull(primaryCandidate.getRouteId());

        assertEquals(12L, queuedCandidate.getVehicleId());
        assertEquals(102L, queuedCandidate.getDriverId());
        assertEquals(DispatchWorkflowContract.ROUTE_CANDIDATE_QUEUED, queuedCandidate.getCandidateStatus());
        assertEquals(2, queuedCandidate.getDisplayOrder());
        assertNull(queuedCandidate.getOfferedAt());
        assertEquals(primaryCandidate.getRouteId(), queuedCandidate.getRouteId());

        ArgumentCaptor<DispatchPlan> planCaptor = ArgumentCaptor.forClass(DispatchPlan.class);
        verify(dispatchPlanMapper).insert(planCaptor.capture());
        assertEquals(2, planCaptor.getValue().getBeforeVehicleCount());
        assertEquals(1, planCaptor.getValue().getAfterVehicleCount());

        Set<Long> eligibleVehicleIds = new HashSet<>();
        fixture.eligibleVehicles().forEach(vehicle -> eligibleVehicleIds.add(vehicle.getId()));
        assertEquals(Set.of(11L, 12L), eligibleVehicleIds);
    }

    @Test
    void generatePlanDoesNotAssignSameBackupVehicleToMultipleRoutes() {
        stubGeneratePlanPersistenceWithBackups();

        dispatchService.generatePlan(902L);

        ArgumentCaptor<RouteOfferCandidate> candidateCaptor = ArgumentCaptor.forClass(RouteOfferCandidate.class);
        verify(routeOfferCandidateMapper, times(3)).insert(candidateCaptor.capture());

        List<RouteOfferCandidate> insertedCandidates = candidateCaptor.getAllValues();
        long queuedCount = insertedCandidates.stream()
                .filter(candidate -> DispatchWorkflowContract.ROUTE_CANDIDATE_QUEUED.equals(candidate.getCandidateStatus()))
                .count();
        long distinctQueuedVehicleCount = insertedCandidates.stream()
                .filter(candidate -> DispatchWorkflowContract.ROUTE_CANDIDATE_QUEUED.equals(candidate.getCandidateStatus()))
                .map(RouteOfferCandidate::getVehicleId)
                .distinct()
                .count();

        assertEquals(1L, queuedCount);
        assertEquals(queuedCount, distinctQueuedVehicleCount);
    }

    @Test
    void confirmPlanRejectsUnclaimedRoutesBeforeAnySideEffects() {
        DispatchPlan plan = new DispatchPlan();
        plan.setId(44L);
        plan.setStatus(DispatchWorkflowContract.PLAN_DRAFT);

        Route offeredRoute = new Route();
        offeredRoute.setId(501L);
        offeredRoute.setPlanId(44L);
        offeredRoute.setStatus(DispatchWorkflowContract.ROUTE_OFFERED);

        when(dispatchPlanMapper.selectById(44L)).thenReturn(plan);
        when(routeMapper.selectList(any())).thenReturn(List.of(offeredRoute));

        BizException exception = assertThrows(BizException.class, () -> dispatchService.confirmPlan(44L));

        assertEquals(ResultCode.BAD_REQUEST.getCode(), exception.getCode());
        verify(routeStopMapper, never()).selectList(any());
        verify(orderServiceClient, never()).changeStatus(any(), any(), any());
        verify(vehicleMapper, never()).updateById(any(Vehicle.class));
        verify(dispatchPlanMapper, never()).updateById(any(DispatchPlan.class));
    }

    @Test
    void acceptRouteWritesWinnerAndExhaustsSiblingOffers() {
        Route route = route(801L, DispatchWorkflowContract.ROUTE_OFFERED, null, null);
        RouteOfferCandidate winner = offeredCandidate(801L, 301L, 401L, 1);

        when(routeOfferCandidateMapper.selectOne(any())).thenReturn(winner);
        when(routeMapper.selectById(801L)).thenReturn(route);
        when(routeMapper.update(any(Route.class), any())).thenReturn(1);
        when(routeOfferCandidateMapper.update(any(RouteOfferCandidate.class), any())).thenReturn(1, 1);

        dispatchService.acceptRoute(801L, 401L, UserRoleContract.DRIVER);

        ArgumentCaptor<Route> routeUpdateCaptor = ArgumentCaptor.forClass(Route.class);
        verify(routeMapper).update(routeUpdateCaptor.capture(), any());
        Route routeUpdate = routeUpdateCaptor.getValue();
        assertEquals(DispatchWorkflowContract.ROUTE_ACCEPTED, routeUpdate.getStatus());
        assertEquals(301L, routeUpdate.getVehicleId());
        assertEquals(401L, routeUpdate.getDriverId());
        assertNotNull(routeUpdate.getUpdatedAt());

        ArgumentCaptor<RouteOfferCandidate> candidateUpdateCaptor = ArgumentCaptor.forClass(RouteOfferCandidate.class);
        verify(routeOfferCandidateMapper, times(2)).update(candidateUpdateCaptor.capture(), any());
        List<RouteOfferCandidate> updates = candidateUpdateCaptor.getAllValues();
        assertEquals(DispatchWorkflowContract.ROUTE_ACCEPTED, updates.get(0).getCandidateStatus());
        assertNotNull(updates.get(0).getRespondedAt());
        assertEquals(DispatchWorkflowContract.ROUTE_OFFER_EXHAUSTED, updates.get(1).getCandidateStatus());
        assertNotNull(updates.get(1).getRespondedAt());
    }

    @Test
    void secondAcceptFailsWithoutOverwritingWinner() {
        Route offeredRoute = route(802L, DispatchWorkflowContract.ROUTE_OFFERED, null, null);
        Route acceptedRoute = route(802L, DispatchWorkflowContract.ROUTE_ACCEPTED, 302L, 402L);
        RouteOfferCandidate firstCandidate = offeredCandidate(802L, 302L, 402L, 1);
        RouteOfferCandidate secondCandidate = offeredCandidate(802L, 303L, 403L, 2);

        when(routeOfferCandidateMapper.selectOne(any())).thenReturn(firstCandidate, secondCandidate);
        when(routeMapper.selectById(802L)).thenReturn(offeredRoute, acceptedRoute);
        when(routeMapper.update(any(Route.class), any())).thenReturn(1);
        when(routeOfferCandidateMapper.update(any(RouteOfferCandidate.class), any())).thenReturn(1, 1);

        dispatchService.acceptRoute(802L, 402L, UserRoleContract.DRIVER);

        BizException exception = assertThrows(BizException.class,
                () -> dispatchService.acceptRoute(802L, 403L, UserRoleContract.DRIVER));

        assertEquals(ResultCode.CONFLICT.getCode(), exception.getCode());
        verify(routeMapper, times(1)).update(any(Route.class), any());
        verify(routeOfferCandidateMapper, times(2)).update(any(RouteOfferCandidate.class), any());
    }

    @Test
    void rejectLastOfferedCandidateMovesRouteToOfferExhausted() {
        RouteOfferCandidate candidate = offeredCandidate(803L, 304L, 404L, 1);

        when(routeOfferCandidateMapper.selectOne(any())).thenReturn(candidate, null);
        when(routeOfferCandidateMapper.update(any(RouteOfferCandidate.class), any())).thenReturn(1);
        when(routeOfferCandidateMapper.selectList(any())).thenReturn(List.of());
        when(routeMapper.update(any(Route.class), any())).thenReturn(1);

        dispatchService.rejectRoute(803L, 404L, UserRoleContract.DRIVER);

        ArgumentCaptor<RouteOfferCandidate> candidateUpdateCaptor = ArgumentCaptor.forClass(RouteOfferCandidate.class);
        verify(routeOfferCandidateMapper).update(candidateUpdateCaptor.capture(), any());
        assertEquals(DispatchWorkflowContract.ROUTE_REJECTED, candidateUpdateCaptor.getValue().getCandidateStatus());
        assertNotNull(candidateUpdateCaptor.getValue().getRespondedAt());

        ArgumentCaptor<Route> routeUpdateCaptor = ArgumentCaptor.forClass(Route.class);
        verify(routeMapper).update(routeUpdateCaptor.capture(), any());
        assertEquals(DispatchWorkflowContract.ROUTE_OFFER_EXHAUSTED, routeUpdateCaptor.getValue().getStatus());
        assertNull(routeUpdateCaptor.getValue().getDriverId());
        assertNull(routeUpdateCaptor.getValue().getVehicleId());
    }

    @Test
    void rejectPromotesNextQueuedCandidateWhenAvailable() {
        RouteOfferCandidate currentCandidate = offeredCandidate(804L, 304L, 404L, 1);
        RouteOfferCandidate queuedCandidate = queuedCandidate(804L, 305L, 405L, 2);

        when(routeOfferCandidateMapper.selectOne(any())).thenReturn(currentCandidate, queuedCandidate);
        when(routeOfferCandidateMapper.update(any(RouteOfferCandidate.class), any())).thenReturn(1, 1);
        when(routeOfferCandidateMapper.selectList(any())).thenReturn(List.of());

        dispatchService.rejectRoute(804L, 404L, UserRoleContract.DRIVER);

        ArgumentCaptor<RouteOfferCandidate> candidateUpdateCaptor = ArgumentCaptor.forClass(RouteOfferCandidate.class);
        verify(routeOfferCandidateMapper, times(2)).update(candidateUpdateCaptor.capture(), any());
        List<RouteOfferCandidate> updates = candidateUpdateCaptor.getAllValues();
        assertEquals(DispatchWorkflowContract.ROUTE_REJECTED, updates.get(0).getCandidateStatus());
        assertEquals(DispatchWorkflowContract.ROUTE_OFFERED, updates.get(1).getCandidateStatus());
        assertNotNull(updates.get(1).getOfferedAt());
        verify(routeMapper, never()).update(any(Route.class), any());
    }

    @Test
    void routeDetailIsSummaryOnlyForOfferedDriverAndFullForAcceptedOwner() {
        Route offeredRoute = route(804L, DispatchWorkflowContract.ROUTE_OFFERED, null, null);
        Route acceptedRoute = route(805L, DispatchWorkflowContract.ROUTE_ACCEPTED, 305L, 405L);
        RouteOfferCandidate offeredCandidate = offeredCandidate(804L, 304L, 404L, 1);
        List<RouteStop> stops = List.of(stop(805L, 1, "pickup", "warehouse"), stop(805L, 2, "delivery", "customer-address"));

        when(routeMapper.selectById(804L)).thenReturn(offeredRoute);
        when(routeMapper.selectById(805L)).thenReturn(acceptedRoute);
        when(routeOfferCandidateMapper.selectOne(any())).thenReturn(offeredCandidate).thenReturn((RouteOfferCandidate) null);
        when(routeStopMapper.selectList(any())).thenReturn(stops);

        RouteDetailDTO summaryDetail = dispatchService.getRouteDetail(804L, 404L, UserRoleContract.DRIVER);
        RouteDetailDTO fullDetail = dispatchService.getRouteDetail(805L, 405L, UserRoleContract.DRIVER);

        assertFalse(summaryDetail.isDetailsVisible());
        assertNull(summaryDetail.getStops());
        assertEquals(DispatchWorkflowContract.ROUTE_OFFERED, summaryDetail.getStatus());

        assertTrue(fullDetail.isDetailsVisible());
        assertNotNull(fullDetail.getStops());
        assertEquals(2, fullDetail.getStops().size());
        assertEquals("customer-address", fullDetail.getStops().get(1).getAddress());
    }

    private GeneratePlanFixture stubGeneratePlanPersistence() {
        List<OrderDTO> orders = List.of(
                order(1L, 23.1001, 113.3001),
                order(2L, 23.1008, 113.3008),
                order(3L, 23.1015, 113.3015));
        List<Vehicle> idleVehicles = List.of(
                vehicle(11L, 101L),
                vehicle(12L, 102L));
        List<Route> savedRoutes = new ArrayList<>();
        DispatchPlan[] savedPlan = new DispatchPlan[1];

        when(orderServiceClient.getPendingOrders(UserRoleContract.DISPATCHER)).thenReturn(Result.success(orders));
        when(vehicleMapper.selectList(any())).thenReturn(idleVehicles);
        when(dispatchPlanMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            DispatchPlan plan = invocation.getArgument(0);
            plan.setId(700L);
            savedPlan[0] = plan;
            return 1;
        }).when(dispatchPlanMapper).insert(any(DispatchPlan.class));
        doAnswer(invocation -> {
            Route route = invocation.getArgument(0);
            route.setId(9000L + savedRoutes.size());
            savedRoutes.add(route);
            return 1;
        }).when(routeMapper).insert(any(Route.class));
        when(dispatchPlanMapper.selectById(700L)).thenAnswer(invocation -> savedPlan[0]);
        when(routeMapper.selectList(any())).thenAnswer(invocation -> savedRoutes);
        when(routeStopMapper.selectList(any())).thenReturn(List.of());

        return new GeneratePlanFixture(700L, List.of(vehicle(11L, 101L), vehicle(12L, 102L)));
    }

    private void stubGeneratePlanPersistenceWithBackups() {
        List<OrderDTO> orders = List.of(
                order(1L, 23.1001, 113.3001),
                order(2L, 23.1008, 113.3008),
                order(3L, 23.1015, 113.3015),
                order(4L, 23.2001, 113.4001),
                order(5L, 23.2008, 113.4008),
                order(6L, 23.2015, 113.4015));
        List<Vehicle> idleVehicles = List.of(
                vehicle(11L, 101L),
                vehicle(12L, 102L),
                vehicle(13L, 103L));
        List<Route> savedRoutes = new ArrayList<>();
        DispatchPlan[] savedPlan = new DispatchPlan[1];

        when(orderServiceClient.getPendingOrders(UserRoleContract.DISPATCHER)).thenReturn(Result.success(orders));
        when(vehicleMapper.selectList(any())).thenReturn(idleVehicles);
        when(dispatchPlanMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            DispatchPlan plan = invocation.getArgument(0);
            plan.setId(701L);
            savedPlan[0] = plan;
            return 1;
        }).when(dispatchPlanMapper).insert(any(DispatchPlan.class));
        doAnswer(invocation -> {
            Route route = invocation.getArgument(0);
            route.setId(9100L + savedRoutes.size());
            savedRoutes.add(route);
            return 1;
        }).when(routeMapper).insert(any(Route.class));
        when(dispatchPlanMapper.selectById(701L)).thenAnswer(invocation -> savedPlan[0]);
        when(routeMapper.selectList(any())).thenAnswer(invocation -> savedRoutes);
        when(routeStopMapper.selectList(any())).thenReturn(List.of());
    }

    private OrderDTO order(Long id, double lat, double lng) {
        OrderDTO order = new OrderDTO();
        order.setId(id);
        order.setReceiverAddress("test-address-" + id);
        order.setReceiverLat(lat);
        order.setReceiverLng(lng);
        return order;
    }

    private Vehicle vehicle(Long id, Long driverId) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setDriverId(driverId);
        vehicle.setStatus(DispatchWorkflowContract.VEHICLE_IDLE);
        return vehicle;
    }

    private Route route(Long id, String status, Long vehicleId, Long driverId) {
        Route route = new Route();
        route.setId(id);
        route.setPlanId(701L);
        route.setStatus(status);
        route.setVehicleId(vehicleId);
        route.setDriverId(driverId);
        route.setEstimatedDistance(BigDecimal.valueOf(18.5));
        route.setEstimatedDuration(42);
        route.setCreatedAt(LocalDateTime.now());
        route.setUpdatedAt(LocalDateTime.now());
        return route;
    }

    private RouteOfferCandidate offeredCandidate(Long routeId, Long vehicleId, Long driverId, int displayOrder) {
        RouteOfferCandidate candidate = new RouteOfferCandidate();
        candidate.setId(routeId * 10 + displayOrder);
        candidate.setRouteId(routeId);
        candidate.setVehicleId(vehicleId);
        candidate.setDriverId(driverId);
        candidate.setCandidateStatus(DispatchWorkflowContract.ROUTE_OFFERED);
        candidate.setDisplayOrder(displayOrder);
        candidate.setOfferedAt(LocalDateTime.now().minusMinutes(5));
        candidate.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        candidate.setUpdatedAt(LocalDateTime.now().minusMinutes(5));
        return candidate;
    }

    private RouteOfferCandidate queuedCandidate(Long routeId, Long vehicleId, Long driverId, int displayOrder) {
        RouteOfferCandidate candidate = offeredCandidate(routeId, vehicleId, driverId, displayOrder);
        candidate.setCandidateStatus(DispatchWorkflowContract.ROUTE_CANDIDATE_QUEUED);
        candidate.setOfferedAt(null);
        return candidate;
    }

    private RouteStop stop(Long routeId, int stopSeq, String stopType, String address) {
        RouteStop stop = new RouteStop();
        stop.setId(routeId * 100 + stopSeq);
        stop.setRouteId(routeId);
        stop.setOrderId(routeId * 1000 + stopSeq);
        stop.setStopSeq(stopSeq);
        stop.setStopType(stopType);
        stop.setAddress(address);
        stop.setLng(BigDecimal.valueOf(113.30 + stopSeq));
        stop.setLat(BigDecimal.valueOf(23.10 + stopSeq));
        return stop;
    }

    private record GeneratePlanFixture(Long planId, List<Vehicle> eligibleVehicles) {
    }
}
