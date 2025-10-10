package com.exotech.urchat.service;

import com.exotech.urchat.dto.authDTOs.*;
import com.exotech.urchat.model.User;
import com.exotech.urchat.repository.UserRepo;
import com.exotech.urchat.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepo userRepo;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request){

        if (userRepo.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .bio(request.getBio())
                .build();
        user.setInitalPfpIndex(request.getPfpIndex());
        user.setInitialPfpBg(request.getPfpBg());
        User savedUser = userRepo.save(user);

        String accessToken = jwtUtil.generateAccessToken(savedUser.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(savedUser.getUsername());

        savedUser.setRefreshToken(refreshToken);
        savedUser.setRefreshTokenExpiry(LocalDateTime.now().plusYears(1));
        userRepo.save(savedUser);

        return new AuthResponse(accessToken, refreshToken, savedUser.getUsername(), savedUser.getEmail(), savedUser.getFullName(),jwtUtil.getAccessTokenExpiry(accessToken), jwtUtil.getRefreshTokenExpiry(refreshToken) );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepo.findByUsername(request.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String accessToken = jwtUtil.generateAccessToken(user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusYears(1));

        return new AuthResponse(accessToken, refreshToken, user.getUsername(), user.getEmail(), user.getFullName(), jwtUtil.getAccessTokenExpiry(accessToken), jwtUtil.getRefreshTokenExpiry(refreshToken));
    }

    @Transactional
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request){
        String refreshToken = request.getRefreshToken();

        User user = userRepo.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        String newAccessToken = jwtUtil.generateAccessToken(user.getUsername());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        LocalDateTime accessTokenExpiry = jwtUtil.getAccessTokenExpiry(newAccessToken);
        LocalDateTime refreshTokenExpiry = LocalDateTime.now().plusYears(1);

        user.setRefreshToken(newRefreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusYears(1));
        userRepo.save(user);

        return new TokenRefreshResponse(newAccessToken, newRefreshToken, accessTokenExpiry, refreshTokenExpiry);
    }

    @Transactional
    public void logout(String username){
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        user.setRefreshToken(null);
        user.setRefreshTokenExpiry(null);
        userRepo.save(user);
    }
}
