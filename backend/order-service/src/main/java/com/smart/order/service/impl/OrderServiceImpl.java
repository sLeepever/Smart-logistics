package com.smart.order.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.order.entity.Order;
import com.smart.order.mapper.OrderMapper;
import com.smart.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;

    @Override
    public Page<Order> listOrders(int page, int size, String status,
                                   String startDate, String endDate, String keyword) {
        // TODO: 多条件分页查询
        throw new UnsupportedOperationException("TODO: implement listOrders");
    }

    @Override
    public Order getOrderById(Long id) {
        // TODO: 查询订单，不存在抛 BizException
        throw new UnsupportedOperationException("TODO: implement getOrderById");
    }

    @Override
    public Order createOrder(Order order, Long creatorId) {
        // TODO: 生成 orderNo，设置 status=pending，保存
        throw new UnsupportedOperationException("TODO: implement createOrder");
    }

    @Override
    public Order updateOrder(Long id, Order order) {
        // TODO: 仅 pending 状态可修改
        throw new UnsupportedOperationException("TODO: implement updateOrder");
    }

    @Override
    public void deleteOrder(Long id) {
        // TODO: 仅 pending 状态可删除
        throw new UnsupportedOperationException("TODO: implement deleteOrder");
    }

    @Override
    public void changeStatus(Long id, String targetStatus, String remark) {
        // TODO: 状态机校验后变更
        throw new UnsupportedOperationException("TODO: implement changeStatus");
    }

    @Override
    public List<Order> getPendingOrders() {
        // TODO: 查询所有 status=pending 的订单，供调度算法使用
        throw new UnsupportedOperationException("TODO: implement getPendingOrders");
    }
}
