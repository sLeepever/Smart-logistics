package com.smart.tracking.controller;

import com.smart.common.result.Result;
import com.smart.tracking.dto.ChatMessageResponse;
import com.smart.tracking.dto.ChatSendRequest;
import com.smart.tracking.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "订单聊天")
@RestController
@RequestMapping("/api/chat/orders")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "查询订单聊天记录")
    @GetMapping("/{orderId}/messages")
    public Result<List<ChatMessageResponse>> getOrderMessages(@PathVariable Long orderId,
                                                              @RequestHeader("X-User-Id") Long userId,
                                                              @RequestHeader("X-User-Role") String userRole) {
        return Result.success(chatService.getOrderMessages(orderId, userId, userRole));
    }

    @Operation(summary = "发送订单聊天消息")
    @PostMapping("/{orderId}/messages")
    public Result<ChatMessageResponse> sendOrderMessage(@PathVariable Long orderId,
                                                        @RequestBody ChatSendRequest request,
                                                        @RequestHeader("X-User-Id") Long userId,
                                                        @RequestHeader("X-User-Role") String userRole) {
        return Result.success(chatService.sendOrderMessage(orderId, request, userId, userRole));
    }
}
