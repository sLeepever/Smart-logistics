package com.smart.tracking.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageResponse {
    private Long id;
    private Long senderUserId;
    private String senderRole;
    private String content;
    private LocalDateTime createdAt;
}
