package com.exotech.urchat.dto.chatDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GroupMembersDTO {

    private String username;
    private String fullName;
    private String pfpIndex;
    private String pfpBg;

    private Boolean isAdmin;
    private Boolean isMember;

}
