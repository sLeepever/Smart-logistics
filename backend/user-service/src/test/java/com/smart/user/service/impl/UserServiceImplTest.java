package com.smart.user.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.common.exception.BizException;
import com.smart.common.result.ResultCode;
import com.smart.user.dto.CreateUserRequest;
import com.smart.user.dto.UserDTO;
import com.smart.user.entity.CustomerProfile;
import com.smart.user.entity.User;
import com.smart.user.mapper.CustomerProfileMapper;
import com.smart.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private CustomerProfileMapper customerProfileMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void createUserCreatesCustomerAndProfileTogether() {
        CreateUserRequest request = customerRequest();
        request.setPassword("Secret@123");

        when(userMapper.selectAnyByUsername("customer01")).thenReturn(null);
        when(passwordEncoder.encode("Secret@123")).thenReturn("ENCODED");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(88L);
            return 1;
        }).when(userMapper).insert(any(User.class));
        when(customerProfileMapper.selectById(88L)).thenReturn(null);

        UserDTO result = userService.createUser(1L, "admin", request);

        assertEquals(88L, result.getId());
        assertEquals("customer", result.getRole());
        assertEquals("王联系人", result.getRealName());
        assertNotNull(result.getCustomerProfile());
        assertEquals("上海市浦东新区 1 号", result.getCustomerProfile().getDefaultAddress());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertEquals("ENCODED", userCaptor.getValue().getPassword());
        assertEquals(1, userCaptor.getValue().getStatus());

        ArgumentCaptor<CustomerProfile> profileCaptor = ArgumentCaptor.forClass(CustomerProfile.class);
        verify(customerProfileMapper).insert(profileCaptor.capture());
        assertEquals(88L, profileCaptor.getValue().getUserId());
        assertEquals("王联系人", profileCaptor.getValue().getContactName());
    }

    @Test
    void listUsersIncludesCustomerProfileForCustomerAccounts() {
        User customer = buildUser(9L, "customer01", "customer", 1);
        Page<User> userPage = new Page<>(1, 10, 1);
        userPage.setRecords(List.of(customer));

        CustomerProfile profile = new CustomerProfile();
        profile.setUserId(9L);
        profile.setContactName("王联系人");
        profile.setDefaultAddress("上海市浦东新区 1 号");

        when(userMapper.selectPage(any(Page.class), any())).thenReturn(userPage);
        when(customerProfileMapper.selectList(any())).thenReturn(List.of(profile));

        Page<UserDTO> result = userService.listUsers(1L, "admin", 1, 10, null, "customer");

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertNotNull(result.getRecords().get(0).getCustomerProfile());
        assertEquals("王联系人", result.getRecords().get(0).getCustomerProfile().getContactName());
    }

    @Test
    void updateUserUpdatesCustomerProfile() {
        User existingUser = buildUser(9L, "customer01", "customer", 1);
        CustomerProfile existingProfile = new CustomerProfile();
        existingProfile.setUserId(9L);
        existingProfile.setContactName("旧联系人");
        existingProfile.setDefaultAddress("旧地址");

        CreateUserRequest request = customerRequest();
        request.setUsername("customer01");
        request.setContactName("新联系人");
        request.setDefaultAddress("新地址");

        when(userMapper.selectById(9L)).thenReturn(existingUser);
        when(userMapper.selectAnyByUsername("customer01")).thenReturn(existingUser);
        when(customerProfileMapper.selectById(9L)).thenReturn(existingProfile);

        UserDTO result = userService.updateUser(1L, "admin", 9L, request);

        assertEquals("新联系人", result.getCustomerProfile().getContactName());
        verify(customerProfileMapper).updateById(existingProfile);
        assertEquals("新地址", existingProfile.getDefaultAddress());
        verify(userMapper).updateById(existingUser);
    }

    @Test
    void updateUserRejectsSelfDisableForCurrentAdmin() {
        User currentAdmin = buildUser(1L, "admin", "admin", 1);
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("admin");
        request.setRole("admin");
        request.setStatus(0);

        when(userMapper.selectById(1L)).thenReturn(currentAdmin);

        BizException exception = assertThrows(BizException.class,
                () -> userService.updateUser(1L, "admin", 1L, request));

        assertEquals(ResultCode.FORBIDDEN.getCode(), exception.getCode());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void deleteUserRejectsCurrentAdminSelfDelete() {
        when(userMapper.selectById(1L)).thenReturn(buildUser(1L, "admin", "admin", 1));

        BizException exception = assertThrows(BizException.class,
                () -> userService.deleteUser(1L, "admin", 1L));

        assertEquals(ResultCode.FORBIDDEN.getCode(), exception.getCode());
        verify(userMapper, never()).deleteById(any());
    }

    @Test
    void resetPasswordEncodesAndUpdatesPassword() {
        User target = buildUser(5L, "customer01", "customer", 1);
        when(userMapper.selectById(5L)).thenReturn(target);
        when(passwordEncoder.encode("New@1234")).thenReturn("ENCODED-PW");

        userService.resetPassword(1L, "admin", 5L, "New@1234");

        assertEquals("ENCODED-PW", target.getPassword());
        verify(userMapper).updateById(target);
    }

    @Test
    void nonCustomerUpdateDoesNotRequireCustomerProfile() {
        User dispatcher = buildUser(6L, "dispatcher01", "dispatcher", 1);
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("dispatcher01");
        request.setRole("dispatcher");
        request.setRealName("张调度");

        when(userMapper.selectById(6L)).thenReturn(dispatcher);
        when(userMapper.selectAnyByUsername("dispatcher01")).thenReturn(dispatcher);

        UserDTO result = userService.updateUser(1L, "admin", 6L, request);

        assertNull(result.getCustomerProfile());
        verify(customerProfileMapper, never()).insert(any(CustomerProfile.class));
    }

    @Test
    void updateUserCleansUpCustomerProfileWhenRoleChangesToNonCustomer() {
        User existingUser = buildUser(9L, "customer01", "customer", 1);
        CustomerProfile existingProfile = new CustomerProfile();
        existingProfile.setUserId(9L);
        existingProfile.setContactName("旧联系人");
        existingProfile.setDefaultAddress("旧地址");
        existingProfile.setDeleted(0);

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("customer01");
        request.setRole("dispatcher");
        request.setRealName("张调度");

        when(userMapper.selectById(9L)).thenReturn(existingUser);
        when(userMapper.selectAnyByUsername("customer01")).thenReturn(existingUser);
        when(customerProfileMapper.selectById(9L)).thenReturn(existingProfile);

        UserDTO result = userService.updateUser(1L, "admin", 9L, request);

        assertEquals("dispatcher", result.getRole());
        assertNull(result.getCustomerProfile());
        assertEquals(1, existingProfile.getDeleted());
        verify(customerProfileMapper).updateById(existingProfile);
    }

    @Test
    void updateUserCleansUpCustomerProfileWhenCustomerIsDisabled() {
        User existingUser = buildUser(9L, "customer01", "customer", 1);
        CustomerProfile existingProfile = new CustomerProfile();
        existingProfile.setUserId(9L);
        existingProfile.setContactName("旧联系人");
        existingProfile.setDefaultAddress("旧地址");
        existingProfile.setDeleted(0);

        CreateUserRequest request = customerRequest();
        request.setStatus(0);

        when(userMapper.selectById(9L)).thenReturn(existingUser);
        when(userMapper.selectAnyByUsername("customer01")).thenReturn(existingUser);
        when(customerProfileMapper.selectById(9L)).thenReturn(existingProfile);

        UserDTO result = userService.updateUser(1L, "admin", 9L, request);

        assertEquals(0, result.getStatus());
        assertNull(result.getCustomerProfile());
        assertEquals(1, existingProfile.getDeleted());
        verify(customerProfileMapper).updateById(existingProfile);
    }

    @Test
    void deleteUserCleansUpCustomerProfileBeforeLogicalDelete() {
        User existingUser = buildUser(9L, "customer01", "customer", 1);
        CustomerProfile existingProfile = new CustomerProfile();
        existingProfile.setUserId(9L);
        existingProfile.setDeleted(0);

        when(userMapper.selectById(9L)).thenReturn(existingUser);
        when(customerProfileMapper.selectById(9L)).thenReturn(existingProfile);

        userService.deleteUser(1L, "admin", 9L);

        assertEquals(1, existingProfile.getDeleted());
        verify(customerProfileMapper).updateById(existingProfile);
        verify(userMapper).deleteById(9L);
    }

    private CreateUserRequest customerRequest() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("customer01");
        request.setRole("customer");
        request.setPhone("13900000009");
        request.setContactName("王联系人");
        request.setCompanyName("华联客户");
        request.setDefaultAddress("上海市浦东新区 1 号");
        request.setRemark("VIP");
        return request;
    }

    private User buildUser(Long id, String username, String role, Integer status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
