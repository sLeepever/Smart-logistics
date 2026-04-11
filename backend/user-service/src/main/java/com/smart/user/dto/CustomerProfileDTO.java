package com.smart.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerProfileDTO {
    private Long userId;
    private String contactName;
    private String companyName;
    private String defaultAddress;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
