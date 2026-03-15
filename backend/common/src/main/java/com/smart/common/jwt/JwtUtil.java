package com.smart.common.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(
                jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 accessToken
     *
     * @param userId 用户 ID
     * @param role   用户角色
     * @return accessToken 字符串
     */
    public String generateAccessToken(Long userId, String role) {
        return buildToken(userId, role, jwtProperties.getAccessTtl());
    }

    /**
     * 生成 refreshToken
     */
    public String generateRefreshToken(Long userId, String role) {
        return buildToken(userId, role, jwtProperties.getRefreshTtl());
    }

    private String buildToken(Long userId, String role, long ttlSeconds) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlSeconds * 1000);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 token，返回 Claims；token 非法或过期时抛出 JwtException
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 获取 token 的 jti（JWT ID），用于黑名单 Key
     */
    public String getJti(String token) {
        return parseToken(token).getId();
    }

    /**
     * 获取 token 剩余有效期（秒），用于设置 Redis TTL
     */
    public long getRemainingTtl(String token) {
        Date expiry = parseToken(token).getExpiration();
        long remaining = (expiry.getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(remaining, 0);
    }

    /**
     * 从 Claims 中提取 userId
     */
    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    /**
     * 从 Claims 中提取 role
     */
    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }
}
