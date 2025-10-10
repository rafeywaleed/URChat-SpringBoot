package com.exotech.urchat.dto.authDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    private String username;
    private String email;
    private String fullName;

    private LocalDateTime accessTokenExpiry;
    private LocalDateTime refreshTokenExpiry;
}

