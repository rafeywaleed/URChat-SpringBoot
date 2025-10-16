package com.exotech.urchat.controller;

import com.exotech.urchat.dto.authDTOs.*;
import com.exotech.urchat.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

//    @PostMapping("/register")
//    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
//        AuthResponse response = authService.register(request);
//        return ResponseEntity.ok(response);
//    }

    @PostMapping("/initiate-registration")
    public ResponseEntity<ApiResponse> initiateRegistration(@RequestBody RegisterRequest request) {
        ApiResponse response = authService.initiateRegistration(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complete-registration")
    public ResponseEntity<AuthResponse> completeRegistration(@RequestBody CompleteRegistrationRequest request) {
        AuthResponse response = authService.completeRegistration(
                request.getRegisterRequest(),
                request.getOtp()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@RequestBody VerifyOtpRequest request) {
        ApiResponse response = authService.initiatePasswordReset(request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@RequestBody ResetPasswordRequest request) {
        ApiResponse response = authService.resetPassword(
                request.getEmail(),
                request.getOtp(),
                request.getNewPassword()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse> resendOtp(@RequestBody VerifyOtpRequest request) {
        ApiResponse response = authService.resendOtp(request.getEmail(), request.getPurpose());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        TokenRefreshResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@AuthenticationPrincipal String username) {
        authService.logout(username);
        return ResponseEntity.ok(new ApiResponse(true, "Logged out successfully"));
    }
}