package com.smart.tracking.service;

import com.smart.tracking.dto.ChatMessageResponse;
import com.smart.tracking.dto.ChatSendRequest;

import java.util.List;

public interface ChatService {
    void authorizeOrderAccess(Long orderId, Long userId, String userRole);

    List<ChatMessageResponse> getOrderMessages(Long orderId, Long userId, String userRole);

    ChatMessageResponse sendOrderMessage(Long orderId, ChatSendRequest request, Long userId, String userRole);
}
