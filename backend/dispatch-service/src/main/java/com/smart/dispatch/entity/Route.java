package com.smart.dispatch.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("routes")
public class Route extends BaseEntity {
    private Long planId;
    private Long vehicleId;
    private Long driverId;
    /** assigned / accepted / in_progress / completed */
    private String status;
    private BigDecimal estimatedDistance;
    private Integer estimatedDuration;
    private BigDecimal actualDistance;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
