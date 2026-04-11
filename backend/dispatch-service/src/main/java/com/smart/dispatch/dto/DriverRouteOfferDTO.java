package com.smart.dispatch.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DriverRouteOfferDTO {
    private Long routeId;
    private Long planId;
    private Long vehicleId;
    private String routeStatus;
    private String candidateStatus;
    private BigDecimal estimatedDistance;
    private Integer estimatedDuration;
    private Integer displayOrder;
    private LocalDateTime offeredAt;
    private boolean detailsVisible;
}
