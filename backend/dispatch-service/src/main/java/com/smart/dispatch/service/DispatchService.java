package com.smart.dispatch.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.dispatch.dto.PlanDetailVO;
import com.smart.dispatch.entity.DispatchPlan;

public interface DispatchService {
    Page<DispatchPlan> listPlans(int page, int size);
    PlanDetailVO getPlanDetail(Long planId);
    PlanDetailVO generatePlan(Long creatorId);
    void confirmPlan(Long planId);
}
