package com.smart.dispatch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("route_offer_candidates")
public class RouteOfferCandidate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long routeId;
    private Long vehicleId;
    private Long driverId;
    private String candidateStatus;
    private LocalDateTime offeredAt;
    private LocalDateTime respondedAt;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
