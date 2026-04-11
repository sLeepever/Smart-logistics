package com.smart.dispatch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("route_stops")
public class RouteStop {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long routeId;
    /** 订单级锚点：后续聊天与权限校验统一锚定 orderId，而非 routeId */
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
