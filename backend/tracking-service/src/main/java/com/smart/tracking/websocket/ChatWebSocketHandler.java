package com.smart.tracking.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.common.exception.BizException;
import com.smart.common.result.ResultCode;
import com.smart.tracking.dto.ChatMessageResponse;
import com.smart.tracking.dto.ChatSendRequest;
import com.smart.tracking.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final OrderChatSessionManager orderChatSessionManager;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("聊天 WebSocket 连接建立: sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode payload = objectMapper.readTree(message.getPayload());
            String type = textValue(payload, "type");
            if (!StringUtils.hasText(type)) {
                sendError(session, ResultCode.BAD_REQUEST.getCode(), "消息类型不能为空");
                return;
            }

            switch (type) {
                case "subscribe" -> handleSubscribe(session, payload);
                case "unsubscribe" -> handleUnsubscribe(session, payload);
                case "chat" -> handleChat(session, payload);
                default -> sendError(session, ResultCode.BAD_REQUEST.getCode(), "不支持的消息类型");
            }
        } catch (BizException ex) {
            sendError(session, ex.getCode(), ex.getMessage());
        } catch (IOException ex) {
            sendError(session, ResultCode.BAD_REQUEST.getCode(), "消息格式错误");
        } catch (Exception ex) {
            log.error("处理聊天 WebSocket 消息失败: sessionId={}", session.getId(), ex);
            sendError(session, ResultCode.INTERNAL_ERROR.getCode(), "消息处理失败");
        }
    }

    private void handleSubscribe(WebSocketSession session, JsonNode payload) {
        Long orderId = requiredOrderId(payload);
        Long userId = requiredUserId(session);
        String userRole = requiredUserRole(session);
        chatService.authorizeOrderAccess(orderId, userId, userRole);
        orderChatSessionManager.subscribe(orderId, session);
    }

    private void handleUnsubscribe(WebSocketSession session, JsonNode payload) {
        requiredUserId(session);
        requiredUserRole(session);
        orderChatSessionManager.unsubscribe(requiredOrderId(payload), session);
    }

    private void handleChat(WebSocketSession session, JsonNode payload) throws IOException {
        Long orderId = requiredOrderId(payload);
        Long userId = requiredUserId(session);
        String userRole = requiredUserRole(session);

        ChatSendRequest request = new ChatSendRequest();
        request.setContent(textValue(payload, "content"));

        ChatMessageResponse persisted = chatService.sendOrderMessage(orderId, request, userId, userRole);
        String broadcastPayload = objectMapper.writeValueAsString(Map.of(
                "type", "chat",
                "orderId", orderId,
                "message", persisted
        ));
        orderChatSessionManager.broadcastToOrder(orderId, broadcastPayload);
    }

    private Long requiredOrderId(JsonNode payload) {
        JsonNode orderIdNode = payload.get("orderId");
        if (orderIdNode == null || !orderIdNode.canConvertToLong()) {
            throw new BizException(ResultCode.BAD_REQUEST, "orderId 无效");
        }
        return orderIdNode.longValue();
    }

    private Long requiredUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get(ChatWebSocketSessionAttributes.USER_ID);
        if (userId instanceof Number number) {
            return number.longValue();
        }
        throw new BizException(ResultCode.UNAUTHORIZED, "缺少用户身份信息");
    }

    private String requiredUserRole(WebSocketSession session) {
        Object userRole = session.getAttributes().get(ChatWebSocketSessionAttributes.USER_ROLE);
        if (userRole instanceof String role && StringUtils.hasText(role)) {
            return role;
        }
        throw new BizException(ResultCode.UNAUTHORIZED, "缺少用户身份信息");
    }

    private String textValue(JsonNode payload, String fieldName) {
        JsonNode fieldNode = payload.get(fieldName);
        return fieldNode == null || fieldNode.isNull() ? null : fieldNode.asText();
    }

    private void sendError(WebSocketSession session, int code, String message) {
        try {
            if (!session.isOpen()) {
                return;
            }
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                    "type", "error",
                    "code", code,
                    "message", message
            ))));
        } catch (IOException ex) {
            log.error("发送聊天 WebSocket 错误消息失败: sessionId={}", session.getId(), ex);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        orderChatSessionManager.removeSession(session);
        log.info("聊天 WebSocket 连接关闭: sessionId={}, status={}", session.getId(), status);
    }
}
