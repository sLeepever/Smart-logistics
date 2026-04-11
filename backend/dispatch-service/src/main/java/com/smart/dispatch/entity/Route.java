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
    /** 可为空：路线在 offer 阶段尚未被最终认领 */
    private Long vehicleId;
    /** 可为空：路线在 offer 阶段尚未被最终认领 */
    private Long driverId;
    /** offered / accepted / rejected / offer_exhausted / in_progress / completed */
    private String status;
    private BigDecimal estimatedDistance;
    private Integer estimatedDuration;
    private BigDecimal actualDistance;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
