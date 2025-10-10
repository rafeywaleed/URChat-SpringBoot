package com.exotech.urchat.dto.webSocketDTOs;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErrorMessage {
    private String code;
    private String message;
    private LocalDateTime timestamp = LocalDateTime.now();

    public ErrorMessage(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
