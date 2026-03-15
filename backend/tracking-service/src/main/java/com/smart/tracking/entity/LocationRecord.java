package com.smart.tracking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("location_records")
public class LocationRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long routeId;
    private Long driverId;
    private BigDecimal lng;
    private BigDecimal lat;
    private BigDecimal speed;
    private BigDecimal heading;
    private LocalDateTime recordedAt;
    private LocalDateTime createdAt;
}
