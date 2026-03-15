package com.smart.user.service;

import com.smart.user.dto.LoginRequest;
import com.smart.user.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    void logout(String token);
    LoginResponse refresh(String refreshToken);
}
