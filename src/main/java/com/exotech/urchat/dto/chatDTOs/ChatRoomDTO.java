package com.exotech.urchat.dto.chatDTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class ChatRoomDTO {

    private String chatId;
    private String chatName;
    private Boolean isGroup;
    private String lastMessage;
    private LocalDateTime lastActivity;

    private String pfpIndex;
    private String pfpBg;

    private int themeIndex;
    private Boolean isDark;

}
