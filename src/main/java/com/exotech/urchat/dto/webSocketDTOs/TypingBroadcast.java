package com.exotech.urchat.dto.webSocketDTOs;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TypingBroadcast {
    private String username;
    private boolean typing;
    private LocalDateTime timestamp = LocalDateTime.now();

    public TypingBroadcast(String username, boolean typing) {
        this.username = username;
        this.typing = typing;
    }
}
