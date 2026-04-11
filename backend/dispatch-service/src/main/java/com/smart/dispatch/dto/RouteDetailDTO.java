package com.smart.dispatch.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RouteDetailDTO {
    private Long routeId;
    private Long planId;
    private Long vehicleId;
    private Long driverId;
    private String status;
    private BigDecimal estimatedDistance;
    private Integer estimatedDuration;
    private boolean detailsVisible;
    private List<RouteStopDTO> stops;

    @Data
    public static class RouteStopDTO {
        private Long id;
        private Long orderId;
        private Integer stopSeq;
        private String stopType;
        private String address;
        private BigDecimal lng;
        private BigDecimal lat;
        private LocalDateTime arrivedAt;
    }
}
