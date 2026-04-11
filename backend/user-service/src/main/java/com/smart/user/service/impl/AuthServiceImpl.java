package com.smart.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.common.exception.BizException;
import com.smart.common.jwt.JwtUtil;
import com.smart.common.result.ResultCode;
import com.smart.user.dto.LoginRequest;
import com.smart.user.dto.LoginResponse;
import com.smart.user.entity.User;
import com.smart.user.mapper.UserMapper;
import com.smart.user.service.AuthService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginRequest request) {
        // 1. 查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
                        .eq(User::getDeleted, 0)
        );
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 2. 校验账号状态
        if (user.getStatus() != 1) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }

        // 3. 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        // 4. 生成双 token
        String accessToken  = jwtUtil.generateAccessToken(user.getId(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getRole());

        log.info("[Auth] 用户登录成功: {} ({})", user.getUsername(), user.getRole());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .build();
    }

    @Override
    public void logout(String token) {
        // 去掉 "Bearer " 前缀
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        try {
            String jti = jwtUtil.getJti(token);
            long ttl  = jwtUtil.getRemainingTtl(token);
            if (ttl > 0) {
                redisTemplate.opsForValue()
                        .set("jwt:blacklist:" + jti, "1", ttl, TimeUnit.SECONDS);
                log.info("[Auth] token 已加入黑名单, jti={}, ttl={}s", jti, ttl);
            }
        } catch (Exception e) {
            log.warn("[Auth] logout 解析 token 失败，忽略: {}", e.getMessage());
        }
    }

    @Override
    public LoginResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtUtil.parseToken(refreshToken);
        } catch (Exception e) {
            throw new BizException(ResultCode.UNAUTHORIZED, "refreshToken 无效或已过期");
        }

        Long userId = jwtUtil.getUserId(claims);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "refreshToken 对应用户不存在");
        }
        if (user.getStatus() != 1) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }

        String newAccessToken = jwtUtil.generateAccessToken(userId, user.getRole());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .userId(userId)
                .username(user.getUsername())
                .realName(user.getRealName())
                .role(user.getRole())
                .build();
    }
}
