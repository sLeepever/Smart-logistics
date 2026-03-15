package com.smart.tracking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LocationReportRequest {
    @NotNull(message = "routeId 不能为空")
    private Long routeId;
    @NotNull(message = "lng 不能为空")
    private BigDecimal lng;
    @NotNull(message = "lat 不能为空")
    private BigDecimal lat;
    private BigDecimal speed;
    private BigDecimal heading;
    @NotNull(message = "recordedAt 不能为空")
    private LocalDateTime recordedAt;
}
