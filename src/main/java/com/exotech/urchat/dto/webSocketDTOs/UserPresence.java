package com.exotech.urchat.dto.webSocketDTOs;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserPresence {
    private String username;
    private String status;
    private LocalDateTime timestamp = LocalDateTime.now();

    public UserPresence(String username, String status) {
        this.username = username;
        this.status = status;
    }
}
