package com.exotech.urchat.dto.authDTOs;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
