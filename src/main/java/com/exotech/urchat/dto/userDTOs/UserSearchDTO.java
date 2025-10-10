package com.exotech.urchat.dto.userDTOs;

import lombok.Data;

@Data
public class UserSearchDTO {
    private String username;
    private String fullName;
    private String pfpIndex;
    private String pfpBg;
}