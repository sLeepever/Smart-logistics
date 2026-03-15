package com.smart.dispatch.dto;

import com.smart.dispatch.entity.DispatchPlan;
import com.smart.dispatch.entity.Route;
import com.smart.dispatch.entity.RouteStop;
import lombok.Data;

import java.util.List;

@Data
public class PlanDetailVO {
    private DispatchPlan plan;
    private List<RouteVO> routes;

    @Data
    public static class RouteVO {
        private Route route;
        private List<RouteStop> stops;
    }
}
