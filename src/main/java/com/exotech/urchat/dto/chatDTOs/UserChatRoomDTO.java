package com.exotech.urchat.dto.chatDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserChatRoomDTO {

    private String chatName;
    private String userFullName;
    private String chatId;
    private Boolean isGroup;
    private String UserBio;

    private String pfpIndex;
    private String pfpBg;
}
