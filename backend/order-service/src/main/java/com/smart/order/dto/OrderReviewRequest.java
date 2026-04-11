package com.smart.order.dto;

import lombok.Data;

@Data
public class OrderReviewRequest {
    private String action;
    private String remark;
}
