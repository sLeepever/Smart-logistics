package com.smart.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.user.dto.CreateUserRequest;
import com.smart.user.dto.UserDTO;

public interface UserService {
    Page<UserDTO> listUsers(Long operatorUserId, String operatorRole, int page, int size, String role, String keyword);
    UserDTO getUserById(Long operatorUserId, String operatorRole, Long id);
    UserDTO createUser(Long operatorUserId, String operatorRole, CreateUserRequest request);
    UserDTO updateUser(Long operatorUserId, String operatorRole, Long id, CreateUserRequest request);
    void deleteUser(Long operatorUserId, String operatorRole, Long id);
    void resetPassword(Long operatorUserId, String operatorRole, Long id, String newPassword);
}
