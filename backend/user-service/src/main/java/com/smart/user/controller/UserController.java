package com.smart.user.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.common.contract.UserRoleContract;
import com.smart.common.exception.BizException;
import com.smart.common.result.ResultCode;
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
            @RequestHeader("X-User-Id") Long operatorUserId,
            @RequestHeader("X-User-Role") String operatorRole,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword) {
        assertAdmin(operatorRole);
        return Result.success(userService.listUsers(operatorUserId, operatorRole, page, size, role, keyword));
    }

    @Operation(summary = "查询单个用户")
    @GetMapping("/{id}")
    public Result<UserDTO> getUser(@RequestHeader("X-User-Id") Long operatorUserId,
                                   @RequestHeader("X-User-Role") String operatorRole,
                                   @PathVariable Long id) {
        assertAdmin(operatorRole);
        return Result.success(userService.getUserById(operatorUserId, operatorRole, id));
    }

    @Operation(summary = "创建用户")
    @PostMapping
    public Result<UserDTO> createUser(@RequestHeader("X-User-Id") Long operatorUserId,
                                      @RequestHeader("X-User-Role") String operatorRole,
                                      @Valid @RequestBody CreateUserRequest request) {
        assertAdmin(operatorRole);
        return Result.success(userService.createUser(operatorUserId, operatorRole, request));
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    public Result<UserDTO> updateUser(@RequestHeader("X-User-Id") Long operatorUserId,
                                      @RequestHeader("X-User-Role") String operatorRole,
                                      @PathVariable Long id,
                                      @Valid @RequestBody CreateUserRequest request) {
        assertAdmin(operatorRole);
        return Result.success(userService.updateUser(operatorUserId, operatorRole, id, request));
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@RequestHeader("X-User-Id") Long operatorUserId,
                                   @RequestHeader("X-User-Role") String operatorRole,
                                   @PathVariable Long id) {
        assertAdmin(operatorRole);
        userService.deleteUser(operatorUserId, operatorRole, id);
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PatchMapping("/{id}/password")
    public Result<Void> resetPassword(@RequestHeader("X-User-Id") Long operatorUserId,
                                      @RequestHeader("X-User-Role") String operatorRole,
                                      @PathVariable Long id,
                                      @RequestBody Map<String, String> body) {
        assertAdmin(operatorRole);
        userService.resetPassword(operatorUserId, operatorRole, id, body.get("newPassword"));
        return Result.success();
    }

    private void assertAdmin(String operatorRole) {
        if (!UserRoleContract.ADMIN.equals(operatorRole)) {
            throw new BizException(ResultCode.FORBIDDEN, "仅管理员可执行该操作");
        }
    }
}
