package com.smart.tracking.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TrackingWebSocketHandlerTest {

    @Test
    void trackingHandlerStillDelegatesSessionLifecycleToTrackingManager() {
        WebSocketSessionManager sessionManager = mock(WebSocketSessionManager.class);
        TrackingWebSocketHandler handler = new TrackingWebSocketHandler(sessionManager);
        WebSocketSession session = mock(WebSocketSession.class);

        handler.afterConnectionEstablished(session);
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        verify(sessionManager).addSession(session);
        verify(sessionManager).removeSession(session);
    }
}
