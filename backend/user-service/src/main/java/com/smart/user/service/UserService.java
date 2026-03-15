package com.smart.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.user.dto.CreateUserRequest;
import com.smart.user.dto.UserDTO;
import com.smart.user.entity.User;

public interface UserService {
    Page<UserDTO> listUsers(int page, int size, String role, String keyword);
    UserDTO getUserById(Long id);
    UserDTO createUser(CreateUserRequest request);
    UserDTO updateUser(Long id, CreateUserRequest request);
    void deleteUser(Long id);
    void resetPassword(Long id, String newPassword);
}
