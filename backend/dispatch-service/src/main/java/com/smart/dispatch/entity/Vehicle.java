package com.smart.dispatch.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.smart.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vehicles")
public class Vehicle extends BaseEntity {
    private String plateNo;
    private String vehicleType;
    private BigDecimal maxWeight;
    private BigDecimal maxVolume;
    private Long driverId;
    /** idle / on_route / maintenance */
    private String status;
}
