package com.exotech.urchat.dto.webSocketDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageDeletionBroadcast {
    private Long messageId;
    private String chatId;
}