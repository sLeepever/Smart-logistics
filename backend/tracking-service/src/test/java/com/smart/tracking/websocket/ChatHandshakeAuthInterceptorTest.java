package com.smart.tracking.websocket;

import com.smart.common.jwt.JwtProperties;
import com.smart.common.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatHandshakeAuthInterceptorTest {

    private StringRedisTemplate stringRedisTemplate;
    private ChatHandshakeAuthInterceptor interceptor;
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtUtil = new JwtUtil(jwtProperties);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        interceptor = new ChatHandshakeAuthInterceptor(jwtUtil, stringRedisTemplate);
    }

    @Test
    void rejectsMissingToken() throws Exception {
        HandshakeResult result = performHandshake("/ws/chat");

        assertFalse(result.allowed());
        assertEquals(401, result.response().getStatus());
    }

    @Test
    void rejectsInvalidToken() throws Exception {
        HandshakeResult result = performHandshake("/ws/chat?token=not-a-jwt");

        assertFalse(result.allowed());
        assertEquals(401, result.response().getStatus());
    }

    @Test
    void rejectsBlacklistedToken() throws Exception {
        String token = jwtUtil.generateAccessToken(9L, "customer");
        when(stringRedisTemplate.hasKey("jwt:blacklist:" + jwtUtil.getJti(token))).thenReturn(true);

        HandshakeResult result = performHandshake("/ws/chat?token=" + token);

        assertFalse(result.allowed());
        assertEquals(401, result.response().getStatus());
    }

    @Test
    void storesUserClaimsOnSuccessfulHandshake() throws Exception {
        String token = jwtUtil.generateAccessToken(18L, "dispatcher");
        when(stringRedisTemplate.hasKey("jwt:blacklist:" + jwtUtil.getJti(token))).thenReturn(false);

        HandshakeResult result = performHandshake("/ws/chat?token=" + token);

        assertTrue(result.allowed());
        assertEquals(18L, result.attributes().get(ChatWebSocketSessionAttributes.USER_ID));
        assertEquals("dispatcher", result.attributes().get(ChatWebSocketSessionAttributes.USER_ROLE));
    }

    private HandshakeResult performHandshake(String uri) throws Exception {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", uri);
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        Map<String, Object> attributes = new HashMap<>();

        boolean allowed = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(servletResponse),
                mock(WebSocketHandler.class),
                attributes
        );
        return new HandshakeResult(allowed, servletResponse, attributes);
    }

    private record HandshakeResult(boolean allowed, MockHttpServletResponse response, Map<String, Object> attributes) {
    }
}
