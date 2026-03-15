package com.smart.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.common.result.Result;
import com.smart.user.dto.CreateUserRequest;
import com.smart.user.dto.UserDTO;
import com.smart.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "用户管理（admin 专用）")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户列表")
    @GetMapping
    public Result<Page<UserDTO>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword) {
        return Result.success(userService.listUsers(page, size, role, keyword));
    }

    @Operation(summary = "查询单个用户")
    @GetMapping("/{id}")
    public Result<UserDTO> getUser(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    public Result<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        return Result.success(userService.createUser(request));
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    public Result<UserDTO> updateUser(@PathVariable Long id,
                                      @Valid @RequestBody CreateUserRequest request) {
        return Result.success(userService.updateUser(id, request));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PatchMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("newPassword"));
        return Result.success();
    }
}
