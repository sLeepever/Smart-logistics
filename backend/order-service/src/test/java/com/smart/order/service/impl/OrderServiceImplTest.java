package com.smart.order.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.common.contract.OrderStatusContract;
import com.smart.common.exception.BizException;
import com.smart.common.result.ResultCode;
import com.smart.order.entity.Order;
import com.smart.order.mapper.OrderMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createOrderSetsPendingReviewForCustomer() {
        Order order = new Order();
        when(orderMapper.selectOne(any())).thenReturn(null);

        Order created = orderService.createOrder(order, 101L, "customer");

        assertEquals(OrderStatusContract.PENDING_REVIEW, created.getStatus());
        assertEquals(101L, created.getCreatorId());
        assertNotNull(created.getOrderNo());
        verify(orderMapper).insert(order);
    }

    @Test
    void getOrderByIdRejectsCrossCustomerAccess() {
        when(orderMapper.selectById(7L)).thenReturn(order(7L, 99L, OrderStatusContract.PENDING_REVIEW));

        BizException ex = assertThrows(BizException.class,
                () -> orderService.getOrderById(7L, 100L, "customer"));

        assertEquals(ResultCode.NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void changeStatusRejectsCustomerCaller() {
        BizException ex = assertThrows(BizException.class,
                () -> orderService.changeStatus(3L, OrderStatusContract.CANCELLED, "nope", 12L, "customer"));

        assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
        verify(orderMapper, never()).selectById(any());
        verify(orderMapper, never()).updateById(any(Order.class));
    }

    @Test
    void updateOrderRejectsCustomerMutationAfterReview() {
        Order existing = order(5L, 22L, OrderStatusContract.PENDING);
        when(orderMapper.selectById(5L)).thenReturn(existing);

        BizException ex = assertThrows(BizException.class,
                () -> orderService.updateOrder(5L, new Order(), 22L, "customer"));

        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        verify(orderMapper, never()).updateById(any(Order.class));
    }

    @Test
    void reviewOrderApproveMovesPendingReviewToPending() {
        when(orderMapper.selectById(9L)).thenReturn(order(9L, 33L, OrderStatusContract.PENDING_REVIEW));

        orderService.reviewOrder(9L, "approve", "approved", 1L, "admin");

        Order update = captureUpdatedOrder();
        assertEquals(9L, update.getId());
        assertEquals(OrderStatusContract.PENDING, update.getStatus());
        assertEquals("approved", update.getRemark());
    }

    @Test
    void reviewOrderRejectMovesPendingReviewToCancelled() {
        when(orderMapper.selectById(10L)).thenReturn(order(10L, 33L, OrderStatusContract.PENDING_REVIEW));

        orderService.reviewOrder(10L, "reject", "materials mismatch", 2L, "dispatcher");

        Order update = captureUpdatedOrder();
        assertEquals(10L, update.getId());
        assertEquals(OrderStatusContract.CANCELLED, update.getStatus());
        assertEquals("materials mismatch", update.getRemark());
    }

    @Test
    void cancelOrderOnlyAllowsPendingReviewForOwner() {
        when(orderMapper.selectById(12L)).thenReturn(order(12L, 55L, OrderStatusContract.PENDING));

        BizException ex = assertThrows(BizException.class,
                () -> orderService.cancelOrder(12L, 55L, "customer"));

        assertEquals(ResultCode.BAD_REQUEST.getCode(), ex.getCode());
        verify(orderMapper, never()).updateById(any(Order.class));
    }

    private Order captureUpdatedOrder() {
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper).updateById(captor.capture());
        return captor.getValue();
    }

    private Order order(Long id, Long creatorId, String status) {
        Order order = new Order();
        order.setId(id);
        order.setCreatorId(creatorId);
        order.setStatus(status);
        order.setDeleted(0);
        return order;
    }
}
