package com.smart.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.order.entity.Order;

import java.util.List;

public interface OrderService {
    Page<Order> listOrders(int page, int size, String status, String startDate, String endDate, String keyword);
    Order getOrderById(Long id);
    Order createOrder(Order order, Long creatorId);
    Order updateOrder(Long id, Order order);
    void deleteOrder(Long id);
    void changeStatus(Long id, String targetStatus, String remark);
    List<Order> getPendingOrders();
}
