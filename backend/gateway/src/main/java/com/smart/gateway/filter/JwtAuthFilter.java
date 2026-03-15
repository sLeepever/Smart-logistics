package com.smart.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final List<String> WHITE_LIST = List.of(
            "/api/auth/login",
            "/api/auth/refresh"
    );

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${jwt.secret:c21hcnRMb2dpc3RpY3MtandrLXNlY3JldC1rZXktZm9yLWhzMjU2LTIwMjY=}")
    private String jwtSecret;

    private final ReactiveStringRedisTemplate redisTemplate;

    public JwtAuthFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 白名单放行
        boolean isWhitelisted = WHITE_LIST.stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
        if (isWhitelisted) {
            return chain.filter(exchange);
        }

        // 获取 Authorization Header
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, 401, "未认证，请先登录");
        }

        String token = authHeader.substring(7);
        Claims claims;
        try {
            claims = parseToken(token);
        } catch (ExpiredJwtException e) {
            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, 1005, "Token 已过期");
        } catch (JwtException e) {
            return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, 1006, "Token 无效");
        }

        String jti = claims.getId();
        String blacklistKey = "jwt:blacklist:" + jti;

        // 检查黑名单（Redis）
        return redisTemplate.hasKey(blacklistKey).flatMap(blacklisted -> {
            if (Boolean.TRUE.equals(blacklisted)) {
                return writeErrorResponse(exchange, HttpStatus.UNAUTHORIZED, 1007, "Token 已被注销");
            }
            // 注入用户信息到下游 Header
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        });
    }

    private Claims parseToken(String token) {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret.getBytes(StandardCharsets.UTF_8));
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status,
                                          int code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("code", code, "message", message, "data", null);
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = "{\"code\":500,\"message\":\"error\"}".getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
