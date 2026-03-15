package com.smart.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.common.exception.BizException;
import com.smart.common.result.ResultCode;
import com.smart.order.entity.Order;
import com.smart.order.mapper.OrderMapper;
import com.smart.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;

    // 合法状态集合
    private static final Set<String> VALID_STATUSES =
            Set.of("pending", "dispatched", "in_progress", "completed", "cancelled", "exception");

    // 状态流转规则：key=当前状态，value=允许变更到的目标状态
    private static final java.util.Map<String, Set<String>> STATUS_TRANSITIONS = java.util.Map.of(
            "pending",     Set.of("dispatched", "cancelled"),
            "dispatched",  Set.of("in_progress", "cancelled", "pending"),
            "in_progress", Set.of("completed", "exception"),
            "exception",   Set.of("pending", "cancelled")
    );

    @Override
    public Page<Order> listOrders(int page, int size, String status,
                                   String startDate, String endDate, String keyword) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getDeleted, 0);

        if (StringUtils.hasText(status)) {
            wrapper.eq(Order::getStatus, status);
        }
        if (StringUtils.hasText(startDate)) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            wrapper.ge(Order::getCreatedAt, start);
        }
        if (StringUtils.hasText(endDate)) {
            LocalDateTime end = LocalDate.parse(endDate).atTime(LocalTime.MAX);
            wrapper.le(Order::getCreatedAt, end);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Order::getOrderNo, keyword)
                    .or()
                    .like(Order::getReceiverName, keyword)
                    .or()
                    .like(Order::getReceiverPhone, keyword)
            );
        }
        wrapper.orderByDesc(Order::getCreatedAt);

        return orderMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Order getOrderById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null || order.getDeleted() == 1) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    @Override
    public Order createOrder(Order order, Long creatorId) {
        order.setOrderNo(generateOrderNo());
        order.setStatus("pending");
        order.setCreatorId(creatorId);
        order.setDeleted(0);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.insert(order);
        return order;
    }

    @Override
    public Order updateOrder(Long id, Order order) {
        Order existing = getOrderById(id);
        if (!"pending".equals(existing.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "只有待调度状态的订单可以修改");
        }
        order.setId(id);
        order.setOrderNo(existing.getOrderNo());
        order.setStatus(existing.getStatus());
        order.setCreatorId(existing.getCreatorId());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return getOrderById(id);
    }

    @Override
    public void deleteOrder(Long id) {
        Order existing = getOrderById(id);
        if (!"pending".equals(existing.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "只有待调度状态的订单可以删除");
        }
        orderMapper.deleteById(id);
    }

    @Override
    public void changeStatus(Long id, String targetStatus, String remark) {
        if (!VALID_STATUSES.contains(targetStatus)) {
            throw new BizException(ResultCode.BAD_REQUEST, "无效的目标状态：" + targetStatus);
        }
        Order existing = getOrderById(id);
        Set<String> allowed = STATUS_TRANSITIONS.getOrDefault(existing.getStatus(), Set.of());
        if (!allowed.contains(targetStatus)) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "订单状态不允许从 " + existing.getStatus() + " 变更为 " + targetStatus);
        }
        Order update = new Order();
        update.setId(id);
        update.setStatus(targetStatus);
        update.setRemark(remark);
        update.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(update);
    }

    @Override
    public List<Order> getPendingOrders() {
        return orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, "pending")
                        .eq(Order::getDeleted, 0)
                        .orderByAsc(Order::getCreatedAt)
        );
    }

    /**
     * 生成订单号：ORD + yyyyMMdd + 4位序号
     */
    private String generateOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "ORD" + date;
        // 查当天最大序号
        Order last = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .likeRight(Order::getOrderNo, prefix)
                        .orderByDesc(Order::getOrderNo)
                        .last("LIMIT 1")
        );
        int seq = 1;
        if (last != null) {
            String lastNo = last.getOrderNo();
            seq = Integer.parseInt(lastNo.substring(prefix.length())) + 1;
        }
        return prefix + String.format("%04d", seq);
    }
}
