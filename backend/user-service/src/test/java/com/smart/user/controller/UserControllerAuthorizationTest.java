package com.smart.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.common.exception.GlobalExceptionHandler;
import com.smart.user.service.UserService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerAuthorizationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final UserService userService = mock(UserService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @ParameterizedTest
    @MethodSource("nonAdminRequests")
    void nonAdminCrudRequestsAreRejected(MockHttpServletRequestBuilder requestBuilder) throws Exception {
        mockMvc.perform(requestBuilder
                        .header("X-User-Id", 2)
                        .header("X-User-Role", "dispatcher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("仅管理员可执行该操作"));

        verifyNoInteractions(userService);
    }

    private static Stream<Arguments> nonAdminRequests() throws Exception {
        String createOrUpdateBody = OBJECT_MAPPER.writeValueAsString(Map.of(
                "username", "customer01",
                "role", "customer",
                "contactName", "王联系人",
                "defaultAddress", "上海市浦东新区 1 号"
        ));
        String resetBody = OBJECT_MAPPER.writeValueAsString(Map.of("newPassword", "New@1234"));

        return Stream.of(
                Arguments.of(get("/api/users")),
                Arguments.of(get("/api/users/1")),
                Arguments.of(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(createOrUpdateBody)),
                Arguments.of(put("/api/users/1").contentType(MediaType.APPLICATION_JSON).content(createOrUpdateBody)),
                Arguments.of(delete("/api/users/1")),
                Arguments.of(patch("/api/users/1/password").contentType(MediaType.APPLICATION_JSON).content(resetBody))
        );
    }
}
