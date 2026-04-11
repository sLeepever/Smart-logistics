package com.smart.tracking.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.common.exception.BizException;
import com.smart.common.result.ResultCode;
import com.smart.tracking.dto.ChatMessageResponse;
import com.smart.tracking.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatWebSocketHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private OrderChatSessionManager orderChatSessionManager;
    private ChatService chatService;
    private ChatWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        orderChatSessionManager = new OrderChatSessionManager();
        chatService = mock(ChatService.class);
        handler = new ChatWebSocketHandler(orderChatSessionManager, chatService, objectMapper);
    }

    @Test
    void subscribeAddsSessionToAuthorizedOrderRoomOnly() throws Exception {
        TestWebSocketSession session = new TestWebSocketSession("session-a");
        session.getAttributes().put(ChatWebSocketSessionAttributes.USER_ID, 7L);
        session.getAttributes().put(ChatWebSocketSessionAttributes.USER_ROLE, "customer");

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"subscribe\",\"orderId\":123}"));

        verify(chatService).authorizeOrderAccess(123L, 7L, "customer");
        assertTrue(orderChatSessionManager.isSubscribed(123L, session));
        assertEquals(1, orderChatSessionManager.getRoomSize(123L));
        assertEquals(0, orderChatSessionManager.getRoomSize(999L));
    }

    @Test
    void unauthorizedSubscribeDoesNotStoreSession() throws Exception {
        TestWebSocketSession session = new TestWebSocketSession("session-b");
        session.getAttributes().put(ChatWebSocketSessionAttributes.USER_ID, 7L);
        session.getAttributes().put(ChatWebSocketSessionAttributes.USER_ROLE, "customer");
        doThrow(new BizException(ResultCode.FORBIDDEN, "无权访问订单聊天"))
                .when(chatService).authorizeOrderAccess(123L, 7L, "customer");

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"subscribe\",\"orderId\":123}"));

        assertFalse(orderChatSessionManager.isSubscribed(123L, session));
        JsonNode error = objectMapper.readTree(session.getLastTextPayload());
        assertEquals("error", error.get("type").asText());
        assertEquals(ResultCode.FORBIDDEN.getCode(), error.get("code").asInt());
    }

    @Test
    void chatPersistsThenBroadcastsPersistedPayloadToRoom() throws Exception {
        TestWebSocketSession subscriberA = new TestWebSocketSession("session-c");
        subscriberA.getAttributes().put(ChatWebSocketSessionAttributes.USER_ID, 21L);
        subscriberA.getAttributes().put(ChatWebSocketSessionAttributes.USER_ROLE, "dispatcher");

        TestWebSocketSession subscriberB = new TestWebSocketSession("session-d");
        subscriberB.getAttributes().put(ChatWebSocketSessionAttributes.USER_ID, 99L);
        subscriberB.getAttributes().put(ChatWebSocketSessionAttributes.USER_ROLE, "customer");

        orderChatSessionManager.subscribe(123L, subscriberA);
        orderChatSessionManager.subscribe(123L, subscriberB);

        ChatMessageResponse persisted = new ChatMessageResponse();
        persisted.setId(88L);
        persisted.setSenderUserId(21L);
        persisted.setSenderRole("dispatcher");
        persisted.setContent("persisted content");
        persisted.setCreatedAt(LocalDateTime.of(2026, 4, 8, 10, 15));
        when(chatService.sendOrderMessage(eq(123L), org.mockito.ArgumentMatchers.any(), eq(21L), eq("dispatcher")))
                .thenReturn(persisted);

        handler.handleTextMessage(subscriberA, new TextMessage("{\"type\":\"chat\",\"orderId\":123,\"content\":\"hello\"}"));

        ArgumentCaptor<com.smart.tracking.dto.ChatSendRequest> requestCaptor = ArgumentCaptor.forClass(com.smart.tracking.dto.ChatSendRequest.class);
        verify(chatService).sendOrderMessage(eq(123L), requestCaptor.capture(), eq(21L), eq("dispatcher"));
        assertEquals("hello", requestCaptor.getValue().getContent());

        JsonNode subscriberAMessage = objectMapper.readTree(subscriberA.getLastTextPayload());
        JsonNode subscriberBMessage = objectMapper.readTree(subscriberB.getLastTextPayload());
        assertEquals(subscriberAMessage, subscriberBMessage);
        assertEquals("chat", subscriberAMessage.get("type").asText());
        assertEquals(123L, subscriberAMessage.get("orderId").asLong());
        assertEquals(88L, subscriberAMessage.get("message").get("id").asLong());
        assertEquals("persisted content", subscriberAMessage.get("message").get("content").asText());
    }

    @Test
    void closingConnectionRemovesSessionFromRooms() throws Exception {
        TestWebSocketSession session = new TestWebSocketSession("session-e");
        orderChatSessionManager.subscribe(123L, session);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertEquals(0, orderChatSessionManager.getRoomSize(123L));
    }

    private static final class TestWebSocketSession implements WebSocketSession {

        private final String id;
        private final Map<String, Object> attributes = new HashMap<>();
        private final List<TextMessage> sentMessages = new ArrayList<>();
        private boolean open = true;

        private TestWebSocketSession(String id) {
            this.id = id;
        }

        String getLastTextPayload() {
            return sentMessages.get(sentMessages.size() - 1).getPayload();
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public URI getUri() {
            return URI.create("ws://localhost/ws/chat");
        }

        @Override
        public org.springframework.http.HttpHeaders getHandshakeHeaders() {
            return new org.springframework.http.HttpHeaders();
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public Principal getPrincipal() {
            return null;
        }

        @Override
        public InetSocketAddress getLocalAddress() {
            return null;
        }

        @Override
        public InetSocketAddress getRemoteAddress() {
            return null;
        }

        @Override
        public String getAcceptedProtocol() {
            return null;
        }

        @Override
        public void setTextMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getTextMessageSizeLimit() {
            return 0;
        }

        @Override
        public void setBinaryMessageSizeLimit(int messageSizeLimit) {
        }

        @Override
        public int getBinaryMessageSizeLimit() {
            return 0;
        }

        @Override
        public List<WebSocketExtension> getExtensions() {
            return List.of();
        }

        @Override
        public void sendMessage(WebSocketMessage<?> message) {
            if (message instanceof TextMessage textMessage) {
                sentMessages.add(textMessage);
            }
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() {
            open = false;
        }

        @Override
        public void close(CloseStatus status) {
            open = false;
        }
    }
}
