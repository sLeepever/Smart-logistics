package com.smart.user.service.impl;

import com.smart.user.dto.LoginRequest;
import com.smart.user.dto.LoginResponse;
import com.smart.user.entity.User;
import com.smart.user.mapper.UserMapper;
import com.smart.common.jwt.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void loginKeepsStablePayloadForExistingAdmin() {
        User admin = buildUser(1L, "admin", "管理员", "admin");
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("Demo@1234");

        when(userMapper.selectOne(any())).thenReturn(admin);
        when(passwordEncoder.matches("Demo@1234", "stored-password")).thenReturn(true);
        when(jwtUtil.generateAccessToken(1L, "admin")).thenReturn("access-admin");
        when(jwtUtil.generateRefreshToken(1L, "admin")).thenReturn("refresh-admin");

        LoginResponse response = authService.login(request);

        assertEquals(1L, response.getUserId());
        assertEquals("admin", response.getUsername());
        assertEquals("管理员", response.getRealName());
        assertEquals("admin", response.getRole());
        assertEquals("access-admin", response.getAccessToken());
        assertEquals("refresh-admin", response.getRefreshToken());
    }

    @Test
    void loginSupportsCustomerWithoutBreakingContract() {
        User customer = buildUser(9L, "customer01", "王联系人", "customer");
        LoginRequest request = new LoginRequest();
        request.setUsername("customer01");
        request.setPassword("Secret@123");

        when(userMapper.selectOne(any())).thenReturn(customer);
        when(passwordEncoder.matches("Secret@123", "stored-password")).thenReturn(true);
        when(jwtUtil.generateAccessToken(9L, "customer")).thenReturn("access-customer");
        when(jwtUtil.generateRefreshToken(9L, "customer")).thenReturn("refresh-customer");

        LoginResponse response = authService.login(request);

        assertEquals(9L, response.getUserId());
        assertEquals("customer01", response.getUsername());
        assertEquals("王联系人", response.getRealName());
        assertEquals("customer", response.getRole());
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
    }

    @Test
    void refreshReturnsStablePayloadForCustomerUser() {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        User customer = buildUser(9L, "customer01", "王联系人", "customer");

        when(jwtUtil.parseToken("refresh-customer")).thenReturn(claims);
        when(jwtUtil.getUserId(claims)).thenReturn(9L);
        when(userMapper.selectById(9L)).thenReturn(customer);
        when(jwtUtil.generateAccessToken(9L, "customer")).thenReturn("new-access");

        LoginResponse response = authService.refresh("refresh-customer");

        assertEquals(9L, response.getUserId());
        assertEquals("customer01", response.getUsername());
        assertEquals("王联系人", response.getRealName());
        assertEquals("customer", response.getRole());
        assertEquals("new-access", response.getAccessToken());
        assertEquals("refresh-customer", response.getRefreshToken());
    }

    private User buildUser(Long id, String username, String realName, String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(realName);
        user.setRole(role);
        user.setStatus(1);
        user.setPassword("stored-password");
        return user;
    }
}
