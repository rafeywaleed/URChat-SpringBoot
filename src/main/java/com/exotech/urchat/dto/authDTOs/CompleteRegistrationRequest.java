package com.exotech.urchat.dto.authDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteRegistrationRequest {
    private RegisterRequest registerRequest;
    private String otp;
}
