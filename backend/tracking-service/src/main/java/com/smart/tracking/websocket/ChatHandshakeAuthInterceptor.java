package com.smart.tracking.websocket;

import com.smart.common.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandshakeAuthInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("token");
        if (!StringUtils.hasText(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        Claims claims;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (JwtException | IllegalArgumentException ex) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String jti = claims.getId();
        try {
            if (StringUtils.hasText(jti)
                    && Boolean.TRUE.equals(stringRedisTemplate.hasKey("jwt:blacklist:" + jti))) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
        } catch (Exception ex) {
            log.error("校验 WebSocket Token 黑名单失败", ex);
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return false;
        }

        attributes.put(ChatWebSocketSessionAttributes.USER_ID, jwtUtil.getUserId(claims));
        attributes.put(ChatWebSocketSessionAttributes.USER_ROLE, jwtUtil.getRole(claims));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
