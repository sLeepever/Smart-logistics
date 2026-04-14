package com.smart.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.common.contract.OrderStatusContract;
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
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_DISPATCHER = "dispatcher";
    private static final String ROLE_CUSTOMER = "customer";
    private static final Set<String> OPERATIONAL_ROLES = Set.of(ROLE_ADMIN, ROLE_DISPATCHER);

    private final OrderMapper orderMapper;

    @Override
    public Page<Order> listOrders(int page, int size, String status,
                                   String startDate, String endDate, String keyword,
                                   String requesterRole) {
        requireOperationalRole(requesterRole);
        return orderMapper.selectPage(new Page<>(page, size),
                buildOrderQueryWrapper(status, startDate, endDate, keyword, null));
    }

    @Override
    public Page<Order> listMyOrders(int page, int size, String status,
                                    String startDate, String endDate, String keyword,
                                    Long requesterId, String requesterRole) {
        requireCustomerRole(requesterRole);
        requireRequesterId(requesterId);
        return orderMapper.selectPage(new Page<>(page, size),
                buildOrderQueryWrapper(status, startDate, endDate, keyword, requesterId));
    }

    @Override
    public Order getOrderById(Long id, Long requesterId, String requesterRole) {
        requireSupportedReaderRole(requesterRole);
        Order order = orderMapper.selectById(id);
        if (order == null || order.getDeleted() == 1) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
        if (isCustomer(requesterRole)) {
            requireRequesterId(requesterId);
            ensureOwner(order, requesterId);
        }
        return order;
    }

    @Override
    public Order createOrder(Order order, Long creatorId, String creatorRole) {
        requireCreatableRole(creatorRole);
        requireRequesterId(creatorId);
        order.setOrderNo(generateOrderNo());
        order.setStatus(isCustomer(creatorRole)
                ? OrderStatusContract.PENDING_REVIEW
                : OrderStatusContract.PENDING);
        order.setCreatorId(creatorId);
        order.setDeleted(0);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.insert(order);
        return order;
    }

    @Override
    public Order updateOrder(Long id, Order order, Long requesterId, String requesterRole) {
        Order existing = getOrderById(id, requesterId, requesterRole);
        if (isCustomer(requesterRole)) {
            if (!OrderStatusContract.PENDING_REVIEW.equals(existing.getStatus())) {
                throw new BizException(ResultCode.BAD_REQUEST, "客户仅可修改待审核订单");
            }
        } else {
            requireOperationalRole(requesterRole);
            if (!OrderStatusContract.PENDING.equals(existing.getStatus())) {
                throw new BizException(ResultCode.BAD_REQUEST, "只有待调度状态的订单可以修改");
            }
        }
        order.setId(id);
        order.setOrderNo(existing.getOrderNo());
        order.setStatus(existing.getStatus());
        order.setCreatorId(existing.getCreatorId());
        order.setCreatedAt(existing.getCreatedAt());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        return getOrderById(id, requesterId, requesterRole);
    }

    @Override
    public void deleteOrder(Long id, Long requesterId, String requesterRole) {
        if (isCustomer(requesterRole)) {
            throw new BizException(ResultCode.FORBIDDEN, "客户订单仅支持取消，不支持删除");
        }
        requireOperationalRole(requesterRole);
        Order existing = getOrderById(id, requesterId, requesterRole);
        if (!OrderStatusContract.PENDING.equals(existing.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "只有待调度状态的订单可以删除");
        }
        orderMapper.deleteById(id);
    }

    @Override
    public void changeStatus(Long id, String targetStatus, String remark, Long requesterId, String requesterRole) {
        if (isCustomer(requesterRole)) {
            throw new BizException(ResultCode.FORBIDDEN, "客户无权通过状态接口修改订单");
        }
        requireOperationalRole(requesterRole);
        if (!OrderStatusContract.VALID_STATUSES.contains(targetStatus)) {
            throw new BizException(ResultCode.BAD_REQUEST, "无效的目标状态：" + targetStatus);
        }
        Order existing = getOrderById(id, requesterId, requesterRole);
        if (OrderStatusContract.PENDING_REVIEW.equals(existing.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "待审核订单请使用审核接口处理");
        }
        Set<String> allowed = OrderStatusContract.STATUS_TRANSITIONS
                .getOrDefault(existing.getStatus(), Set.of());
        if (!allowed.contains(targetStatus)) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "订单状态不允许从 " + existing.getStatus() + " 变更为 " + targetStatus);
        }
        updateOrderStatus(id, targetStatus, remark);
    }

    @Override
    public void cancelOrder(Long id, Long requesterId, String requesterRole) {
        requireCustomerRole(requesterRole);
        Order existing = getOrderById(id, requesterId, requesterRole);
        if (!OrderStatusContract.PENDING_REVIEW.equals(existing.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "客户仅可取消待审核订单");
        }
        updateOrderStatus(id, OrderStatusContract.CANCELLED, existing.getRemark());
    }

    @Override
    public void reviewOrder(Long id, String action, String remark, Long requesterId, String requesterRole) {
        requireOperationalRole(requesterRole);
        Order existing = getOrderById(id, requesterId, requesterRole);
        if (!OrderStatusContract.PENDING_REVIEW.equals(existing.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "只有待审核订单可以审核");
        }
        String normalizedAction = normalizeAction(action);
        String targetStatus = switch (normalizedAction) {
            case "approve" -> OrderStatusContract.PENDING;
            case "reject" -> OrderStatusContract.CANCELLED;
            default -> throw new BizException(ResultCode.BAD_REQUEST, "无效的审核动作：" + action);
        };
        updateOrderStatus(id, targetStatus, remark);
    }

    @Override
    public List<Order> getPendingOrders() {
        return orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, OrderStatusContract.PENDING)
                        .eq(Order::getDeleted, 0)
                        .orderByAsc(Order::getCreatedAt)
        );
    }

    private LambdaQueryWrapper<Order> buildOrderQueryWrapper(String status, String startDate,
                                                             String endDate, String keyword,
                                                             Long creatorId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getDeleted, 0);

        if (creatorId != null) {
            wrapper.eq(Order::getCreatorId, creatorId);
        }
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
                    .like(Order::getGoodsName, keyword)
                    .or()
                    .like(Order::getReceiverPhone, keyword)
            );
        }
        return wrapper.orderByDesc(Order::getCreatedAt);
    }

    private void updateOrderStatus(Long id, String targetStatus, String remark) {
        Order update = new Order();
        update.setId(id);
        update.setStatus(targetStatus);
        update.setRemark(remark);
        update.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(update);
    }

    private void requireOperationalRole(String requesterRole) {
        if (!OPERATIONAL_ROLES.contains(requesterRole)) {
            throw new BizException(ResultCode.FORBIDDEN, "仅管理员或调度员可执行该操作");
        }
    }

    private void requireCustomerRole(String requesterRole) {
        if (!isCustomer(requesterRole)) {
            throw new BizException(ResultCode.FORBIDDEN, "仅客户可执行该操作");
        }
    }

    private void requireSupportedReaderRole(String requesterRole) {
        if (isCustomer(requesterRole) || OPERATIONAL_ROLES.contains(requesterRole)) {
            return;
        }
        throw new BizException(ResultCode.FORBIDDEN, "当前角色无权访问订单");
    }

    private void requireCreatableRole(String creatorRole) {
        if (isCustomer(creatorRole) || OPERATIONAL_ROLES.contains(creatorRole)) {
            return;
        }
        throw new BizException(ResultCode.FORBIDDEN, "当前角色无权创建订单");
    }

    private void requireRequesterId(Long requesterId) {
        if (requesterId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "缺少用户身份信息");
        }
    }

    private void ensureOwner(Order order, Long requesterId) {
        if (!requesterId.equals(order.getCreatorId())) {
            throw new BizException(ResultCode.NOT_FOUND, "订单不存在");
        }
    }

    private boolean isCustomer(String requesterRole) {
        return ROLE_CUSTOMER.equals(requesterRole);
    }

    private String normalizeAction(String action) {
        return action == null ? "" : action.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 生成订单号：ORD + yyyyMMdd + 4位序号（使用 PostgreSQL 序列防并发竞争）
     */
    private String generateOrderNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = orderMapper.nextOrderNoSeq();
        return "ORD" + date + String.format("%04d", seq % 10000 == 0 ? 10000 : seq % 10000);
    }
}
