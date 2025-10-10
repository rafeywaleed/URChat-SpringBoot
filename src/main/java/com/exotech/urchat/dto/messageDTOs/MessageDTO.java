package com.exotech.urchat.dto.messageDTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDTO {
    private Long id;
    private String content;
    private String sender;
    private String chatId;
    private LocalDateTime timestamp;
    private boolean isOwnMessage;
}
