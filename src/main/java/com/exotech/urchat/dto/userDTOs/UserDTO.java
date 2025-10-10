package com.exotech.urchat.dto.userDTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
public class UserDTO {

    private String username;
    private String fullName;
    private String bio;
    private String pfpIndex;
    private String pfpBg;
    private LocalDateTime joinedAt;
}
