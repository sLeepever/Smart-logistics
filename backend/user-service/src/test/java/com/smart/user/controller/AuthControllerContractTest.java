package com.smart.user.controller;

import com.smart.common.exception.GlobalExceptionHandler;
import com.smart.user.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerContractTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(mock(AuthService.class)))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void meEndpointStillReturnsCurrentUserContract() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("X-User-Id", 9)
                        .header("X-User-Role", "customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(9))
                .andExpect(jsonPath("$.data.role").value("customer"));
    }
}
