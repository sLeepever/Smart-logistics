package com.smart.tracking.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrackingWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketSessionManager sessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionManager.addSession(session);
        log.info("WebSocket 连接建立: sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 客户端可发送订阅消息，如 {"type":"subscribe","routeId":123}
        log.debug("收到 WebSocket 消息: {}", message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionManager.removeSession(session);
        log.info("WebSocket 连接关闭: sessionId={}, status={}", session.getId(), status);
    }
}
