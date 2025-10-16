package com.exotech.urchat.dto.webSocketDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatDeletionBroadcast {
    private String chatId;
    private String deletedBy;
    private String reason; // "admin_deleted", "empty_group", "user_left_last", "scheduled_cleanup"
    private String chatName; // For display purposes
    private LocalDateTime deletedAt;

    public ChatDeletionBroadcast(String chatId, String deletedBy, String reason) {
        this.chatId = chatId;
        this.deletedBy = deletedBy;
        this.reason = reason;
        this.deletedAt = LocalDateTime.now();
    }
}