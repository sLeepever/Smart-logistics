package com.smart.dispatch.contract;

import com.smart.common.contract.DispatchWorkflowContract;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DispatchWorkflowContractTest {

    @Test
    void routeOfferStatusesAreFrozenForLaterTasks() {
        assertTrue(DispatchWorkflowContract.ROUTE_STATUSES.contains(DispatchWorkflowContract.ROUTE_OFFERED));
        assertTrue(DispatchWorkflowContract.ROUTE_STATUSES.contains(DispatchWorkflowContract.ROUTE_ACCEPTED));
        assertTrue(DispatchWorkflowContract.ROUTE_STATUSES.contains(DispatchWorkflowContract.ROUTE_REJECTED));
        assertTrue(DispatchWorkflowContract.ROUTE_STATUSES.contains(DispatchWorkflowContract.ROUTE_OFFER_EXHAUSTED));
    }

    @Test
    void chatAnchorRemainsOrderScoped() {
        assertEquals("orderId", DispatchWorkflowContract.CHAT_ANCHOR_FIELD);
    }
}
