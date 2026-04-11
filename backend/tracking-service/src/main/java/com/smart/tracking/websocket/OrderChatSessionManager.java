package com.smart.tracking.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class OrderChatSessionManager {

    private final Map<Long, Set<WebSocketSession>> orderRooms = new ConcurrentHashMap<>();
    private final Map<String, Set<Long>> sessionOrders = new ConcurrentHashMap<>();

    public void subscribe(Long orderId, WebSocketSession session) {
        orderRooms.computeIfAbsent(orderId, key -> ConcurrentHashMap.newKeySet()).add(session);
        sessionOrders.computeIfAbsent(session.getId(), key -> ConcurrentHashMap.newKeySet()).add(orderId);
    }

    public void unsubscribe(Long orderId, WebSocketSession session) {
        Set<WebSocketSession> sessions = orderRooms.get(orderId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                orderRooms.remove(orderId, sessions);
            }
        }

        Set<Long> orders = sessionOrders.get(session.getId());
        if (orders != null) {
            orders.remove(orderId);
            if (orders.isEmpty()) {
                sessionOrders.remove(session.getId(), orders);
            }
        }
    }

    public void removeSession(WebSocketSession session) {
        Set<Long> orders = sessionOrders.remove(session.getId());
        if (orders == null) {
            return;
        }

        for (Long orderId : orders) {
            Set<WebSocketSession> sessions = orderRooms.get(orderId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    orderRooms.remove(orderId, sessions);
                }
            }
        }
    }

    public void broadcastToOrder(Long orderId, String payload) {
        Set<WebSocketSession> sessions = orderRooms.get(orderId);
        if (sessions == null) {
            return;
        }
        sessions.forEach(session -> sendMessage(session, payload));
    }

    int getRoomSize(Long orderId) {
        Set<WebSocketSession> sessions = orderRooms.get(orderId);
        return sessions == null ? 0 : sessions.size();
    }

    boolean isSubscribed(Long orderId, WebSocketSession session) {
        Set<WebSocketSession> sessions = orderRooms.get(orderId);
        return sessions != null && sessions.contains(session);
    }

    private void sendMessage(WebSocketSession session, String payload) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(payload));
            }
        } catch (IOException e) {
            log.error("订单聊天消息发送失败: sessionId={}", session.getId(), e);
        }
    }
}
