package com.exotech.urchat.dto.chatDTOs;

import com.exotech.urchat.dto.userDTOs.UserDTO;
import com.exotech.urchat.dto.userDTOs.UserSearchDTO;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GroupChatRoomDTO {

    private String chatName;
    private String chatId;
    private Boolean isGroup;
    private String adminUsername;
    private List<GroupMembersDTO> groupMembers;
    private List<GroupMembersDTO> memberRequests;

    private String pfpIndex;
    private String pfpBg;
}
