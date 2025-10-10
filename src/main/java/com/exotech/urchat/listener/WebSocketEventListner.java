package com.exotech.urchat.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListner {

    private final SimpMessageSendingOperations messagingTemplate;

    @EventListener
    public void handleWebSocketListner(SessionConnectedEvent event){
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String username = headerAccessor.getUser() != null
                ? headerAccessor.getUser().getName()
                : "unkown";

        String sessionId = headerAccessor.getSessionId();
        log.info("WebSocket connected - User: {}, Session: {}", username, sessionId);
    }

    @EventListener
    public void handleWebSocketDisconnectListner(SessionDisconnectEvent event){
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : "unknown";
        String sessionId = headerAccessor.getSessionId();

        log.info("WebSocket disconnected - User: {}, Session: {}", username, sessionId);
    }

}
