package com.exotech.urchat.config;

import com.exotech.urchat.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if(accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())){
            List<String> authHeaders = accessor.getNativeHeader("Authorization");

            if(authHeaders!=null  && !authHeaders.isEmpty()){
                String token = authHeaders.get(0);

                if(token != null && token.startsWith("Bearer ")){
                    token = token.substring(7);

                    if(jwtUtil.validateToken(token)){
                        String username = jwtUtil.extractUsername(token);

                        UsernamePasswordAuthenticationToken auth =
                                new UsernamePasswordAuthenticationToken(username, null,  null);

//                        SecurityContextHolder.getContext().setAuthentication(auth);
                        accessor.setUser(auth);

                        log.info("WebSocket authenticated for user: {}", username);
                    } else {
                        log.warn("Invalid JWT token in WebSocket connection");
                        throw new RuntimeException("Invalid authentication token");
                    }
                }
            } else {
                log.warn("No authorization header in WebSocket connection");
                throw new RuntimeException("Authentication requires");
            }
        }
        return message;
    }
}
