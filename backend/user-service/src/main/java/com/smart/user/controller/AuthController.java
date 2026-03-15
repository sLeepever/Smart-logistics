package com.smart.user.controller;

import com.smart.common.result.Result;
import com.smart.user.dto.LoginRequest;
import com.smart.user.dto.LoginResponse;
import com.smart.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "认证模块")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "用户登出")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.logout(token);
        return Result.success();
    }

    @Operation(summary = "刷新 Token")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        return Result.success(authService.refresh(body.get("refreshToken")));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<Map<String, Object>> me(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        return Result.success(Map.of("userId", userId, "role", role));
    }
}
