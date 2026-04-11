package com.smart.common.contract;

import java.util.Map;
import java.util.Set;

public final class OrderStatusContract {

    public static final String PENDING_REVIEW = "pending_review";
    public static final String PENDING = "pending";
    public static final String DISPATCHED = "dispatched";
    public static final String IN_PROGRESS = "in_progress";
    public static final String COMPLETED = "completed";
    public static final String CANCELLED = "cancelled";
    public static final String EXCEPTION = "exception";

    public static final Set<String> VALID_STATUSES = Set.of(
            PENDING_REVIEW,
            PENDING,
            DISPATCHED,
            IN_PROGRESS,
            COMPLETED,
            CANCELLED,
            EXCEPTION
    );

    public static final Map<String, Set<String>> STATUS_TRANSITIONS = Map.of(
            PENDING_REVIEW, Set.of(PENDING, CANCELLED),
            PENDING, Set.of(DISPATCHED, CANCELLED),
            DISPATCHED, Set.of(IN_PROGRESS, CANCELLED, PENDING),
            IN_PROGRESS, Set.of(COMPLETED, EXCEPTION),
            EXCEPTION, Set.of(PENDING, CANCELLED)
    );

    private OrderStatusContract() {
    }
}
