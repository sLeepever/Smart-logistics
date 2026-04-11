package com.smart.common.contract;

import java.util.Set;

public final class DispatchWorkflowContract {

    public static final String CHAT_ANCHOR_FIELD = "orderId";

    public static final String ROUTE_OFFERED = "offered";
    public static final String ROUTE_ACCEPTED = "accepted";
    public static final String ROUTE_REJECTED = "rejected";
    public static final String ROUTE_OFFER_EXHAUSTED = "offer_exhausted";
    public static final String ROUTE_CANDIDATE_QUEUED = "queued";
    public static final String ROUTE_IN_PROGRESS = "in_progress";
    public static final String ROUTE_COMPLETED = "completed";

    public static final Set<String> ROUTE_STATUSES = Set.of(
            ROUTE_OFFERED,
            ROUTE_ACCEPTED,
            ROUTE_REJECTED,
            ROUTE_OFFER_EXHAUSTED,
            ROUTE_IN_PROGRESS,
            ROUTE_COMPLETED
    );

    public static final String PLAN_DRAFT = "draft";
    public static final String PLAN_CONFIRMED = "confirmed";
    public static final String PLAN_EXECUTING = "executing";
    public static final String PLAN_COMPLETED = "completed";
    public static final String PLAN_CANCELLED = "cancelled";

    public static final Set<String> PLAN_STATUSES = Set.of(
            PLAN_DRAFT,
            PLAN_CONFIRMED,
            PLAN_EXECUTING,
            PLAN_COMPLETED,
            PLAN_CANCELLED
    );

    public static final String VEHICLE_IDLE = "idle";
    public static final String VEHICLE_ON_ROUTE = "on_route";
    public static final String VEHICLE_MAINTENANCE = "maintenance";

    public static final Set<String> VEHICLE_STATUSES = Set.of(
            VEHICLE_IDLE,
            VEHICLE_ON_ROUTE,
            VEHICLE_MAINTENANCE
    );

    private DispatchWorkflowContract() {
    }
}
