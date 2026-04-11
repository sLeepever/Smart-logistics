package com.smart.tracking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.common.exception.GlobalExceptionHandler;
import com.smart.common.handler.MybatisPlusMetaObjectHandler;
import com.smart.common.result.Result;
import com.smart.common.result.ResultCode;
import com.smart.tracking.dto.ChatSendRequest;
import com.smart.tracking.feign.DispatchServiceClient;
import com.smart.tracking.feign.OrderServiceClient;
import com.smart.tracking.feign.dto.DriverRoute;
import com.smart.tracking.feign.dto.DriverRouteStop;
import com.smart.tracking.feign.dto.DriverRouteView;
import com.smart.tracking.feign.dto.OrderSummary;
import com.smart.tracking.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ChatControllerIntegrationTest.TestApp.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private OrderServiceClient orderServiceClient;

    @MockBean
    private DispatchServiceClient dispatchServiceClient;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM order_chat_messages");
        jdbcTemplate.execute("DELETE FROM order_chat_conversations");
    }

    @Test
    void customerCanReadAndSendMessagesForOwnOrder() throws Exception {
        stubOrderAccess(101L, 7L, "customer", true);

        mockMvc.perform(get("/api/chat/orders/101/messages")
                        .header("X-User-Id", 7)
                        .header("X-User-Role", "customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));

        ChatSendRequest request = new ChatSendRequest();
        request.setContent("  customer message  ");

        mockMvc.perform(post("/api/chat/orders/101/messages")
                        .header("X-User-Id", 7)
                        .header("X-User-Role", "customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.senderUserId").value(7))
                .andExpect(jsonPath("$.data.senderRole").value("customer"))
                .andExpect(jsonPath("$.data.content").value("customer message"));

        mockMvc.perform(get("/api/chat/orders/101/messages")
                        .header("X-User-Id", 7)
                        .header("X-User-Role", "customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].content").value("customer message"));
    }

    @Test
    void dispatcherCanReadAndSendMessagesForExistingOrder() throws Exception {
        stubOrderAccess(202L, 2L, "dispatcher", true);

        ChatSendRequest request = new ChatSendRequest();
        request.setContent("dispatcher reply");

        mockMvc.perform(post("/api/chat/orders/202/messages")
                        .header("X-User-Id", 2)
                        .header("X-User-Role", "dispatcher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.senderRole").value("dispatcher"));

        mockMvc.perform(get("/api/chat/orders/202/messages")
                        .header("X-User-Id", 2)
                        .header("X-User-Role", "dispatcher"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void driverCanReadAndSendMessagesWhenOrderBelongsToAcceptedRoute() throws Exception {
        stubDriverRoutes(33L, List.of(routeView("accepted", 303L), routeView("completed", 999L)));

        ChatSendRequest request = new ChatSendRequest();
        request.setContent("driver update");

        mockMvc.perform(post("/api/chat/orders/303/messages")
                        .header("X-User-Id", 33)
                        .header("X-User-Role", "driver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.senderRole").value("driver"));

        mockMvc.perform(get("/api/chat/orders/303/messages")
                        .header("X-User-Id", 33)
                        .header("X-User-Role", "driver"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void unrelatedCustomerIsRejected() throws Exception {
        stubOrderAccess(404L, 70L, "customer", false);

        mockMvc.perform(get("/api/chat/orders/404/messages")
                        .header("X-User-Id", 70)
                        .header("X-User-Role", "customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.NOT_FOUND.getCode()));

        ChatSendRequest request = new ChatSendRequest();
        request.setContent("blocked");

        mockMvc.perform(post("/api/chat/orders/404/messages")
                        .header("X-User-Id", 70)
                        .header("X-User-Role", "customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.NOT_FOUND.getCode()));

        assertConversationCount(0);
    }

    @Test
    void unrelatedDriverIsRejected() throws Exception {
        stubDriverRoutes(55L, List.of(routeView("accepted", 808L)));

        mockMvc.perform(get("/api/chat/orders/909/messages")
                        .header("X-User-Id", 55)
                        .header("X-User-Role", "driver"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.FORBIDDEN.getCode()));

        ChatSendRequest request = new ChatSendRequest();
        request.setContent("blocked");

        mockMvc.perform(post("/api/chat/orders/909/messages")
                        .header("X-User-Id", 55)
                        .header("X-User-Role", "driver")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.FORBIDDEN.getCode()));

        assertConversationCount(0);
    }

    @Test
    void messagesAreReturnedInChronologicalOrder() throws Exception {
        stubOrderAccess(606L, 1L, "admin", true);

        Long conversationId = insertConversation(606L, LocalDateTime.of(2026, 4, 8, 9, 0));
        insertMessage(conversationId, 1L, "admin", "second", LocalDateTime.of(2026, 4, 8, 9, 5));
        insertMessage(conversationId, 9L, "customer", "first", LocalDateTime.of(2026, 4, 8, 9, 1));
        insertMessage(conversationId, 33L, "driver", "third", LocalDateTime.of(2026, 4, 8, 9, 10));

        mockMvc.perform(get("/api/chat/orders/606/messages")
                        .header("X-User-Id", 1)
                        .header("X-User-Role", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].content").value("first"))
                .andExpect(jsonPath("$.data[1].content").value("second"))
                .andExpect(jsonPath("$.data[2].content").value("third"));
    }

    private void stubOrderAccess(Long orderId, Long userId, String userRole, boolean allowed) {
        Result<OrderSummary> result = allowed ? Result.success(orderSummary(orderId, userId))
                : Result.fail(ResultCode.NOT_FOUND, "订单不存在");
        when(orderServiceClient.getOrder(eq(orderId), eq(userId), eq(userRole))).thenReturn(result);
    }

    private void stubDriverRoutes(Long driverId, List<DriverRouteView> routes) {
        when(dispatchServiceClient.getDriverRoutes(eq(driverId), eq("driver"))).thenReturn(Result.success(routes));
    }

    private OrderSummary orderSummary(Long orderId, Long creatorId) {
        OrderSummary orderSummary = new OrderSummary();
        orderSummary.setId(orderId);
        orderSummary.setCreatorId(creatorId);
        return orderSummary;
    }

    private DriverRouteView routeView(String routeStatus, Long orderId) {
        DriverRoute route = new DriverRoute();
        route.setId(orderId + 1000);
        route.setStatus(routeStatus);

        DriverRouteStop stop = new DriverRouteStop();
        stop.setOrderId(orderId);

        DriverRouteView routeView = new DriverRouteView();
        routeView.setRoute(route);
        routeView.setStops(List.of(stop));
        return routeView;
    }

    private void assertConversationCount(int expected) {
        Integer actual = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_chat_conversations", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    private Long insertConversation(Long orderId, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "INSERT INTO order_chat_conversations(order_id, created_at, updated_at, deleted) VALUES (?, ?, ?, 0)",
                orderId, createdAt, createdAt
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM order_chat_conversations WHERE order_id = ?",
                Long.class,
                orderId
        );
    }

    private void insertMessage(Long conversationId, Long senderUserId, String senderRole, String content, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "INSERT INTO order_chat_messages(conversation_id, sender_user_id, sender_role, content, created_at, deleted) VALUES (?, ?, ?, ?, ?, 0)",
                conversationId, senderUserId, senderRole, content, createdAt
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan("com.smart.tracking.mapper")
    @Import({
            ChatController.class,
            ChatServiceImpl.class,
            GlobalExceptionHandler.class,
            MybatisPlusMetaObjectHandler.class
    })
    static class TestApp {
    }
}
