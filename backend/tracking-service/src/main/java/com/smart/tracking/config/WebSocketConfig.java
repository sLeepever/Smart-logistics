package com.smart.tracking.config;

import com.smart.tracking.websocket.ChatHandshakeAuthInterceptor;
import com.smart.tracking.websocket.ChatWebSocketHandler;
import com.smart.tracking.websocket.TrackingWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final TrackingWebSocketHandler trackingWebSocketHandler;
    private final ChatWebSocketHandler chatWebSocketHandler;
    private final ChatHandshakeAuthInterceptor chatHandshakeAuthInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(trackingWebSocketHandler, "/ws/tracking")
                .setAllowedOrigins("*");

        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(chatHandshakeAuthInterceptor)
                .setAllowedOrigins("*");
    }
}
