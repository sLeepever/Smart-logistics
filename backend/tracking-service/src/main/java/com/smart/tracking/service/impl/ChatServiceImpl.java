package com.smart.tracking.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.common.contract.UserRoleContract;
import com.smart.common.exception.BizException;
import com.smart.common.result.Result;
import com.smart.common.result.ResultCode;
import com.smart.tracking.dto.ChatMessageResponse;
import com.smart.tracking.dto.ChatSendRequest;
import com.smart.tracking.entity.OrderChatConversation;
import com.smart.tracking.entity.OrderChatMessage;
import com.smart.tracking.feign.DispatchServiceClient;
import com.smart.tracking.feign.OrderServiceClient;
import com.smart.tracking.feign.dto.DriverRouteStop;
import com.smart.tracking.feign.dto.DriverRouteView;
import com.smart.tracking.feign.dto.OrderSummary;
import com.smart.tracking.mapper.OrderChatConversationMapper;
import com.smart.tracking.mapper.OrderChatMessageMapper;
import com.smart.tracking.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final Set<String> DRIVER_ALLOWED_ROUTE_STATUSES = Set.of("accepted", "in_progress", "completed");

    private final OrderChatConversationMapper conversationMapper;
    private final OrderChatMessageMapper messageMapper;
    private final OrderServiceClient orderServiceClient;
    private final DispatchServiceClient dispatchServiceClient;

    @Override
    public void authorizeOrderAccess(Long orderId, Long userId, String userRole) {
        authorize(orderId, userId, userRole);
    }

    @Override
    public List<ChatMessageResponse> getOrderMessages(Long orderId, Long userId, String userRole) {
        authorizeOrderAccess(orderId, userId, userRole);

        OrderChatConversation conversation = findConversationByOrderId(orderId);
        if (conversation == null) {
            return List.of();
        }

        return messageMapper.selectList(new LambdaQueryWrapper<OrderChatMessage>()
                        .eq(OrderChatMessage::getConversationId, conversation.getId())
                        .orderByAsc(OrderChatMessage::getCreatedAt)
                        .orderByAsc(OrderChatMessage::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ChatMessageResponse sendOrderMessage(Long orderId, ChatSendRequest request, Long userId, String userRole) {
        authorizeOrderAccess(orderId, userId, userRole);

        String content = normalizeContent(request);
        OrderChatConversation conversation = getOrCreateConversation(orderId);

        OrderChatMessage message = new OrderChatMessage();
        message.setConversationId(conversation.getId());
        message.setSenderUserId(userId);
        message.setSenderRole(userRole);
        message.setContent(content);
        message.setDeleted(0);
        messageMapper.insert(message);

        OrderChatConversation conversationUpdate = new OrderChatConversation();
        conversationUpdate.setId(conversation.getId());
        conversationUpdate.setUpdatedAt(LocalDateTime.now());
        conversationMapper.updateById(conversationUpdate);

        return toResponse(messageMapper.selectById(message.getId()));
    }

    private void authorize(Long orderId, Long userId, String userRole) {
        requireUserContext(userId, userRole);

        if (UserRoleContract.isOperational(userRole) || UserRoleContract.CUSTOMER.equals(userRole)) {
            authorizeThroughOrderService(orderId, userId, userRole);
            return;
        }
        if (UserRoleContract.DRIVER.equals(userRole)) {
            authorizeDriver(orderId, userId, userRole);
            return;
        }

        throw new BizException(ResultCode.FORBIDDEN, "当前角色无权访问订单聊天");
    }

    private void authorizeThroughOrderService(Long orderId, Long userId, String userRole) {
        Result<OrderSummary> result = orderServiceClient.getOrder(orderId, userId, userRole);
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
            int code = result == null ? ResultCode.INTERNAL_ERROR.getCode() : result.getCode();
            String message = result == null ? "订单校验失败" : result.getMessage();
            throw new BizException(code, StringUtils.hasText(message) ? message : "订单校验失败");
        }
    }

    private void authorizeDriver(Long orderId, Long userId, String userRole) {
        Result<List<DriverRouteView>> result = dispatchServiceClient.getDriverRoutes(userId, userRole);
        if (result == null || result.getCode() != ResultCode.SUCCESS.getCode() || result.getData() == null) {
            throw new BizException(ResultCode.FORBIDDEN, "当前司机无权访问该订单聊天");
        }

        boolean allowed = result.getData().stream()
                .filter(routeView -> routeView.getRoute() != null)
                .filter(routeView -> DRIVER_ALLOWED_ROUTE_STATUSES.contains(routeView.getRoute().getStatus()))
                .map(DriverRouteView::getStops)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .map(DriverRouteStop::getOrderId)
                .filter(Objects::nonNull)
                .anyMatch(orderId::equals);
        if (!allowed) {
            throw new BizException(ResultCode.FORBIDDEN, "当前司机无权访问该订单聊天");
        }
    }

    private void requireUserContext(Long userId, String userRole) {
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "缺少用户身份信息");
        }
        if (!UserRoleContract.isSupported(userRole)) {
            throw new BizException(ResultCode.FORBIDDEN, "当前角色无权访问订单聊天");
        }
    }

    private String normalizeContent(ChatSendRequest request) {
        String content = request == null ? null : request.getContent();
        String normalized = content == null ? "" : content.trim();
        if (!StringUtils.hasText(normalized)) {
            throw new BizException(ResultCode.BAD_REQUEST, "消息内容不能为空");
        }
        return normalized;
    }

    private OrderChatConversation getOrCreateConversation(Long orderId) {
        OrderChatConversation existing = findConversationByOrderId(orderId);
        if (existing != null) {
            return existing;
        }

        OrderChatConversation conversation = new OrderChatConversation();
        conversation.setOrderId(orderId);
        conversation.setDeleted(0);
        try {
            conversationMapper.insert(conversation);
            return conversation;
        } catch (DuplicateKeyException ex) {
            OrderChatConversation concurrentConversation = findConversationByOrderId(orderId);
            if (concurrentConversation != null) {
                return concurrentConversation;
            }
            throw ex;
        }
    }

    private OrderChatConversation findConversationByOrderId(Long orderId) {
        return conversationMapper.selectOne(new LambdaQueryWrapper<OrderChatConversation>()
                .eq(OrderChatConversation::getOrderId, orderId)
                .last("LIMIT 1"));
    }

    private ChatMessageResponse toResponse(OrderChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setId(message.getId());
        response.setSenderUserId(message.getSenderUserId());
        response.setSenderRole(message.getSenderRole());
        response.setContent(message.getContent());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }
}
