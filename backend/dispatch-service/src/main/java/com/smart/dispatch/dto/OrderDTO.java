package com.smart.dispatch.dto;

import lombok.Data;

@Data
public class OrderDTO {
    private Long id;
    private String orderNo;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private double receiverLng;
    private double receiverLat;
    private double weight;
    private double volume;
    private String status;
}
