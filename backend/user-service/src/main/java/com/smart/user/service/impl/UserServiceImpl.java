package com.smart.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.common.contract.UserRoleContract;
import com.smart.common.exception.BizException;
import com.smart.common.result.ResultCode;
import com.smart.user.dto.CustomerProfileDTO;
import com.smart.user.dto.CreateUserRequest;
import com.smart.user.dto.UserDTO;
import com.smart.user.entity.CustomerProfile;
import com.smart.user.entity.User;
import com.smart.user.mapper.CustomerProfileMapper;
import com.smart.user.mapper.UserMapper;
import com.smart.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final CustomerProfileMapper customerProfileMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<UserDTO> listUsers(Long operatorUserId, String operatorRole, int page, int size, String role, String keyword) {
        assertAdmin(operatorUserId, operatorRole);

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<User>()
                .eq(hasText(role), User::getRole, normalize(role));
        if (hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like(User::getUsername, normalize(keyword))
                    .or()
                    .like(User::getRealName, normalize(keyword))
                    .or()
                    .like(User::getPhone, normalize(keyword)));
        }
        queryWrapper.orderByDesc(User::getCreatedAt);

        Page<User> userPage = userMapper.selectPage(new Page<>(page, size), queryWrapper);
        List<UserDTO> records = toUserDTOs(userPage.getRecords());

        Page<UserDTO> resultPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public UserDTO getUserById(Long operatorUserId, String operatorRole, Long id) {
        assertAdmin(operatorUserId, operatorRole);
        return toUserDTO(requireUser(id), findCustomerProfile(id));
    }

    @Override
    @Transactional
    public UserDTO createUser(Long operatorUserId, String operatorRole, CreateUserRequest request) {
        assertAdmin(operatorUserId, operatorRole);
        if (!hasText(request.getPassword())) {
            throw new BizException(ResultCode.BAD_REQUEST, "密码不能为空");
        }

        assertUsernameAvailable(request.getUsername(), null);
        User user = new User();
        applyUserChanges(user, request, true);
        user.setPassword(passwordEncoder.encode(request.getPassword().trim()));
        userMapper.insert(user);

        CustomerProfile customerProfile = upsertCustomerProfileIfNeeded(user.getId(), user.getRole(), user.getStatus(), request);
        return toUserDTO(user, customerProfile);
    }

    @Override
    @Transactional
    public UserDTO updateUser(Long operatorUserId, String operatorRole, Long id, CreateUserRequest request) {
        assertAdmin(operatorUserId, operatorRole);
        User existingUser = requireUser(id);
        boolean wasCustomer = UserRoleContract.CUSTOMER.equals(existingUser.getRole());
        if (Objects.equals(operatorUserId, id) && Integer.valueOf(0).equals(request.getStatus())) {
            throw new BizException(ResultCode.FORBIDDEN, "不能禁用当前登录管理员账号");
        }

        assertUsernameAvailable(request.getUsername(), id);
        applyUserChanges(existingUser, request, false);
        userMapper.updateById(existingUser);

        cleanupCustomerProfileIfInactive(id, wasCustomer, existingUser.getRole(), existingUser.getStatus());

        CustomerProfile customerProfile = upsertCustomerProfileIfNeeded(id, existingUser.getRole(), existingUser.getStatus(), request);
        return toUserDTO(existingUser, customerProfile);
    }

    @Override
    @Transactional
    public void deleteUser(Long operatorUserId, String operatorRole, Long id) {
        assertAdmin(operatorUserId, operatorRole);
        User user = requireUser(id);
        if (Objects.equals(operatorUserId, id)) {
            throw new BizException(ResultCode.FORBIDDEN, "不能删除当前登录管理员账号");
        }
        cleanupCustomerProfileIfInactive(id, UserRoleContract.CUSTOMER.equals(user.getRole()), user.getRole(), 0);
        userMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void resetPassword(Long operatorUserId, String operatorRole, Long id, String newPassword) {
        assertAdmin(operatorUserId, operatorRole);
        User user = requireUser(id);
        if (!hasText(newPassword)) {
            throw new BizException(ResultCode.BAD_REQUEST, "新密码不能为空");
        }
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userMapper.updateById(user);
    }

    private void assertAdmin(Long operatorUserId, String operatorRole) {
        if (operatorUserId == null || !UserRoleContract.ADMIN.equals(operatorRole)) {
            throw new BizException(ResultCode.FORBIDDEN, "仅管理员可执行该操作");
        }
    }

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    private void assertUsernameAvailable(String username, Long currentUserId) {
        if (!hasText(username)) {
            throw new BizException(ResultCode.BAD_REQUEST, "用户名不能为空");
        }
        User existing = userMapper.selectAnyByUsername(username.trim());
        if (existing != null && !Objects.equals(existing.getId(), currentUserId)) {
            throw new BizException(ResultCode.USERNAME_DUPLICATE);
        }
    }

    private void applyUserChanges(User user, CreateUserRequest request, boolean creating) {
        String role = normalize(request.getRole());
        if (!UserRoleContract.isSupported(role)) {
            throw new BizException(ResultCode.BAD_REQUEST, "不支持的角色类型");
        }

        user.setUsername(normalize(request.getUsername()));
        user.setRealName(resolveRealName(request, role));
        user.setPhone(normalizeNullable(request.getPhone()));
        user.setRole(role);
        user.setStatus(resolveStatus(request.getStatus(), user.getStatus(), creating));
        if (creating) {
            user.setDeleted(0);
        }
    }

    private Integer resolveStatus(Integer requestedStatus, Integer currentStatus, boolean creating) {
        if (requestedStatus == null) {
            return creating ? 1 : currentStatus;
        }
        if (!Objects.equals(requestedStatus, 0) && !Objects.equals(requestedStatus, 1)) {
            throw new BizException(ResultCode.BAD_REQUEST, "状态只能是 0 或 1");
        }
        return requestedStatus;
    }

    private String resolveRealName(CreateUserRequest request, String role) {
        if (hasText(request.getRealName())) {
            return normalize(request.getRealName());
        }
        if (UserRoleContract.CUSTOMER.equals(role) && hasText(request.getContactName())) {
            return normalize(request.getContactName());
        }
        return null;
    }

    private CustomerProfile upsertCustomerProfileIfNeeded(Long userId, String role, Integer status, CreateUserRequest request) {
        if (!UserRoleContract.CUSTOMER.equals(role) || Integer.valueOf(0).equals(status)) {
            return null;
        }
        validateCustomerProfile(request);

        CustomerProfile customerProfile = customerProfileMapper.selectById(userId);
        if (customerProfile == null) {
            customerProfile = new CustomerProfile();
            customerProfile.setUserId(userId);
            applyCustomerProfileChanges(customerProfile, request);
            customerProfile.setDeleted(0);
            customerProfileMapper.insert(customerProfile);
            return customerProfile;
        }

        applyCustomerProfileChanges(customerProfile, request);
        customerProfileMapper.updateById(customerProfile);
        return customerProfile;
    }

    private void cleanupCustomerProfileIfInactive(Long userId, boolean wasCustomer, String currentRole, Integer currentStatus) {
        boolean shouldCleanup = wasCustomer
                && (!UserRoleContract.CUSTOMER.equals(currentRole) || Integer.valueOf(0).equals(currentStatus));
        if (!shouldCleanup) {
            return;
        }

        CustomerProfile customerProfile = customerProfileMapper.selectById(userId);
        if (customerProfile == null || Integer.valueOf(1).equals(customerProfile.getDeleted())) {
            return;
        }

        customerProfile.setDeleted(1);
        customerProfileMapper.updateById(customerProfile);
    }

    private void validateCustomerProfile(CreateUserRequest request) {
        if (!hasText(request.getContactName())) {
            throw new BizException(ResultCode.BAD_REQUEST, "客户联系人不能为空");
        }
        if (!hasText(request.getDefaultAddress())) {
            throw new BizException(ResultCode.BAD_REQUEST, "客户默认地址不能为空");
        }
    }

    private void applyCustomerProfileChanges(CustomerProfile customerProfile, CreateUserRequest request) {
        customerProfile.setContactName(normalize(request.getContactName()));
        customerProfile.setCompanyName(normalizeNullable(request.getCompanyName()));
        customerProfile.setDefaultAddress(normalize(request.getDefaultAddress()));
        customerProfile.setRemark(normalizeNullable(request.getRemark()));
        customerProfile.setDeleted(0);
    }

    private CustomerProfile findCustomerProfile(Long userId) {
        return customerProfileMapper.selectById(userId);
    }

    private List<UserDTO> toUserDTOs(List<User> users) {
        if (users == null || users.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> customerIds = users.stream()
                .filter(user -> UserRoleContract.CUSTOMER.equals(user.getRole()))
                .map(User::getId)
                .toList();

        Map<Long, CustomerProfile> profileMap = customerIds.isEmpty()
                ? Collections.emptyMap()
                : customerProfileMapper.selectList(new LambdaQueryWrapper<CustomerProfile>()
                        .in(CustomerProfile::getUserId, customerIds))
                .stream()
                .collect(Collectors.toMap(CustomerProfile::getUserId, Function.identity()));

        return users.stream()
                .map(user -> toUserDTO(user, profileMap.get(user.getId())))
                .toList();
    }

    private UserDTO toUserDTO(User user, CustomerProfile customerProfile) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setRealName(user.getRealName());
        userDTO.setPhone(user.getPhone());
        userDTO.setRole(user.getRole());
        userDTO.setStatus(user.getStatus());
        userDTO.setCreatedAt(user.getCreatedAt());
        userDTO.setUpdatedAt(user.getUpdatedAt());
        if (UserRoleContract.CUSTOMER.equals(user.getRole()) && customerProfile != null) {
            userDTO.setCustomerProfile(toCustomerProfileDTO(customerProfile));
        }
        return userDTO;
    }

    private CustomerProfileDTO toCustomerProfileDTO(CustomerProfile customerProfile) {
        CustomerProfileDTO dto = new CustomerProfileDTO();
        dto.setUserId(customerProfile.getUserId());
        dto.setContactName(customerProfile.getContactName());
        dto.setCompanyName(customerProfile.getCompanyName());
        dto.setDefaultAddress(customerProfile.getDefaultAddress());
        dto.setRemark(customerProfile.getRemark());
        dto.setCreatedAt(customerProfile.getCreatedAt());
        dto.setUpdatedAt(customerProfile.getUpdatedAt());
        return dto;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizeNullable(String value) {
        return hasText(value) ? value.trim() : null;
    }
}
