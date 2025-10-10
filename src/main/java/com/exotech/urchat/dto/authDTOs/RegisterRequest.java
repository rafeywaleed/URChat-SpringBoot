package com.exotech.urchat.dto.authDTOs;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String fullName;
    private String bio;
    private String pfpIndex;
    private String pfpBg;
}
