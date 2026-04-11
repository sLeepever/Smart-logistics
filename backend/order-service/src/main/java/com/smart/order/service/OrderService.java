package com.smart.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.order.entity.Order;

import java.util.List;

public interface OrderService {
    Page<Order> listOrders(int page, int size, String status, String startDate, String endDate, String keyword,
                           String requesterRole);
    Page<Order> listMyOrders(int page, int size, String status, String startDate, String endDate, String keyword,
                             Long requesterId, String requesterRole);
    Order getOrderById(Long id, Long requesterId, String requesterRole);
    Order createOrder(Order order, Long creatorId, String creatorRole);
    Order updateOrder(Long id, Order order, Long requesterId, String requesterRole);
    void deleteOrder(Long id, Long requesterId, String requesterRole);
    void changeStatus(Long id, String targetStatus, String remark, Long requesterId, String requesterRole);
    void cancelOrder(Long id, Long requesterId, String requesterRole);
    void reviewOrder(Long id, String action, String remark, Long requesterId, String requesterRole);
    List<Order> getPendingOrders();
}
