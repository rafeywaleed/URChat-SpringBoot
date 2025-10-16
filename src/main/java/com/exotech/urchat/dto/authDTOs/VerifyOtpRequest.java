package com.exotech.urchat.dto.authDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {
    private String email;
    private String otp;
    private String purpose; // "REGISTRATION" or "PASSWORD_RESET"
}
