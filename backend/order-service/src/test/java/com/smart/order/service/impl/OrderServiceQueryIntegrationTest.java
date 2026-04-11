package com.smart.order.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.common.contract.OrderStatusContract;
import com.smart.order.entity.Order;
import com.smart.order.mapper.OrderMapper;
import com.smart.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = OrderServiceQueryIntegrationTest.TestApp.class)
@ActiveProfiles("test")
@Transactional
@Import(OrderServiceImpl.class)
class OrderServiceQueryIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM orders");
    }

    @Test
    void listMyOrdersReturnsOnlyOwnedNonDeletedOrders() {
        insertOrder("ORD-001", 88L, OrderStatusContract.PENDING_REVIEW, 0, LocalDateTime.of(2026, 4, 8, 9, 0));
        insertOrder("ORD-002", 88L, OrderStatusContract.PENDING_REVIEW, 0, LocalDateTime.of(2026, 4, 8, 10, 0));
        insertOrder("ORD-003", 99L, OrderStatusContract.PENDING_REVIEW, 0, LocalDateTime.of(2026, 4, 8, 11, 0));
        insertOrder("ORD-004", 88L, OrderStatusContract.PENDING_REVIEW, 1, LocalDateTime.of(2026, 4, 8, 12, 0));

        Page<Order> page = orderService.listMyOrders(1, 10, null, null, null, null, 88L, "customer");

        assertEquals(2, page.getRecords().size());
        assertEquals(List.of("ORD-002", "ORD-001"), page.getRecords().stream().map(Order::getOrderNo).toList());
        assertEquals(List.of(88L, 88L), page.getRecords().stream().map(Order::getCreatorId).toList());
    }

    @Test
    void getPendingOrdersReturnsOnlyPendingNonDeletedOrdersInAscendingCreateTime() {
        insertOrder("ORD-P1", 1L, OrderStatusContract.PENDING, 0, LocalDateTime.of(2026, 4, 8, 8, 0));
        insertOrder("ORD-R1", 1L, OrderStatusContract.PENDING_REVIEW, 0, LocalDateTime.of(2026, 4, 8, 9, 0));
        insertOrder("ORD-PX", 1L, OrderStatusContract.PENDING, 1, LocalDateTime.of(2026, 4, 8, 10, 0));
        insertOrder("ORD-P2", 1L, OrderStatusContract.PENDING, 0, LocalDateTime.of(2026, 4, 8, 11, 0));

        List<Order> pendingOrders = orderService.getPendingOrders();

        assertEquals(List.of("ORD-P1", "ORD-P2"), pendingOrders.stream().map(Order::getOrderNo).toList());
        assertEquals(List.of(OrderStatusContract.PENDING, OrderStatusContract.PENDING),
                pendingOrders.stream().map(Order::getStatus).toList());
    }

    @Test
    void listMyOrdersSupportsGoodsNameKeywordSearch() {
        insertOrder("ORD-G1", 88L, OrderStatusContract.PENDING_REVIEW, 0, LocalDateTime.of(2026, 4, 8, 8, 0), "冷链疫苗");
        insertOrder("ORD-G2", 88L, OrderStatusContract.PENDING_REVIEW, 0, LocalDateTime.of(2026, 4, 8, 9, 0), "办公耗材");

        Page<Order> page = orderService.listMyOrders(1, 10, null, null, null, "疫苗", 88L, "customer");

        assertEquals(List.of("ORD-G1"), page.getRecords().stream().map(Order::getOrderNo).toList());
    }

    private void insertOrder(String orderNo, Long creatorId, String status, int deleted, LocalDateTime createdAt) {
        insertOrder(orderNo, creatorId, status, deleted, createdAt, "测试货物");
    }

    private void insertOrder(String orderNo, Long creatorId, String status, int deleted, LocalDateTime createdAt, String goodsName) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setSenderName("Sender");
        order.setSenderPhone("13800000000");
        order.setSenderAddress("上海市浦东新区 1 号");
        order.setSenderLng(BigDecimal.valueOf(121.500000));
        order.setSenderLat(BigDecimal.valueOf(31.200000));
        order.setReceiverName("Receiver");
        order.setReceiverPhone("13900000000");
        order.setReceiverAddress("上海市徐汇区 2 号");
        order.setReceiverLng(BigDecimal.valueOf(121.400000));
        order.setReceiverLat(BigDecimal.valueOf(31.100000));
        order.setGoodsName(goodsName);
        order.setWeight(BigDecimal.valueOf(12.5));
        order.setVolume(BigDecimal.valueOf(3.2));
        order.setStatus(status);
        order.setRemark("test");
        order.setCreatorId(creatorId);
        order.setDeleted(deleted);
        order.setCreatedAt(createdAt);
        order.setUpdatedAt(createdAt);
        orderMapper.insert(order);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            RedisAutoConfiguration.class,
            RedisRepositoriesAutoConfiguration.class,
    })
    @MapperScan("com.smart.order.mapper")
    static class TestApp {
    }
}
