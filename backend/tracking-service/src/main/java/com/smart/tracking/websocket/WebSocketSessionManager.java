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
public class WebSocketSessionManager {

    /** routeId -> 订阅该路线的调度员 Session 集合 */
    private final Map<Long, Set<WebSocketSession>> routeSessionMap = new ConcurrentHashMap<>();
    /** 所有在线调度员 Session（用于广播全部路线最新位置） */
    private final Set<WebSocketSession> allSessions = ConcurrentHashMap.newKeySet();

    public void addSession(WebSocketSession session) {
        allSessions.add(session);
    }

    public void removeSession(WebSocketSession session) {
        allSessions.remove(session);
        routeSessionMap.values().forEach(sessions -> sessions.remove(session));
    }

    public void broadcast(String message) {
        allSessions.forEach(session -> sendMessage(session, message));
    }

    public void broadcastToRoute(Long routeId, String message) {
        Set<WebSocketSession> sessions = routeSessionMap.get(routeId);
        if (sessions != null) {
            sessions.forEach(session -> sendMessage(session, message));
        }
    }

    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            log.error("WebSocket 消息发送失败: sessionId={}", session.getId(), e);
        }
    }
}
