package com.smart.order.contract;

import com.smart.common.contract.OrderStatusContract;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusContractTest {

    @Test
    void validStatusesIncludePendingReview() {
        assertTrue(OrderStatusContract.VALID_STATUSES.contains(OrderStatusContract.PENDING_REVIEW));
    }

    @Test
    void pendingReviewTransitionsMatchReviewWorkflow() {
        assertEquals(
                java.util.Set.of(OrderStatusContract.PENDING, OrderStatusContract.CANCELLED),
                OrderStatusContract.STATUS_TRANSITIONS.get(OrderStatusContract.PENDING_REVIEW)
        );
    }
}
