package com.exotech.urchat.dto.webSocketDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatDeletionBroadcast {
    private String chatId;
    private String deletedBy;
    private String reason; // "user_deleted", "admin_deleted", "empty_group"
}