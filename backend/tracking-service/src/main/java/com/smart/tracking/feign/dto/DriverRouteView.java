package com.smart.tracking.feign.dto;

import lombok.Data;

import java.util.List;

@Data
public class DriverRouteView {
    private DriverRoute route;
    private List<DriverRouteStop> stops;
}
