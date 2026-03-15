package com.smart.user.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.user.dto.CreateUserRequest;
import com.smart.user.dto.UserDTO;
import com.smart.user.mapper.UserMapper;
import com.smart.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public Page<UserDTO> listUsers(int page, int size, String role, String keyword) {
        // TODO: 分页查询用户列表，支持 role/keyword 筛选
        throw new UnsupportedOperationException("TODO: implement listUsers");
    }

    @Override
    public UserDTO getUserById(Long id) {
        // TODO: 查询用户详情，not found 抛 BizException
        throw new UnsupportedOperationException("TODO: implement getUserById");
    }

    @Override
    public UserDTO createUser(CreateUserRequest request) {
        // TODO: 检查用户名唯一性，BCrypt 加密密码，保存
        throw new UnsupportedOperationException("TODO: implement createUser");
    }

    @Override
    public UserDTO updateUser(Long id, CreateUserRequest request) {
        // TODO: 更新用户信息
        throw new UnsupportedOperationException("TODO: implement updateUser");
    }

    @Override
    public void deleteUser(Long id) {
        // TODO: 逻辑删除
        throw new UnsupportedOperationException("TODO: implement deleteUser");
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        // TODO: BCrypt 加密后更新密码
        throw new UnsupportedOperationException("TODO: implement resetPassword");
    }
}
