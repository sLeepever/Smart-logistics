package com.smart.dispatch.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("route_stops")
public class RouteStop {
    private Long id;
    private Long routeId;
    private Long orderId;
    private Integer stopSeq;
    /** pickup / delivery */
    private String stopType;
    private String address;
    private BigDecimal lng;
    private BigDecimal lat;
    private LocalDateTime arrivedAt;
    private LocalDateTime createdAt;
}
