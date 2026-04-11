package com.smart.dispatch.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.common.entity.BaseEntity;
import com.smart.dispatch.handler.JsonbTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "dispatch_plans", autoResultMap = true)
public class DispatchPlan extends BaseEntity {
    private String planNo;
    /** draft / confirmed / executing / completed / cancelled */
    private String status;
    private Integer totalOrders;
    private Integer totalRoutes;
    private BigDecimal beforeTotalDistance;
    private BigDecimal afterTotalDistance;
    private Integer beforeVehicleCount;
    private Integer afterVehicleCount;
    /** JSON 格式算法参数快照 */
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String algorithmParams;
    private Long createdBy;
    private LocalDateTime confirmedAt;
    private LocalDateTime completedAt;
}
