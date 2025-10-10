package com.exotech.urchat.dto.webSocketDTOs;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReadReceiptBroadcast {
    private String username;
    private Long messageId;
    private LocalDateTime readAt;

    public ReadReceiptBroadcast(String username, Long messageId, LocalDateTime readAt) {
        this.username = username;
        this.messageId = messageId;
        this.readAt = readAt;
    }
}
