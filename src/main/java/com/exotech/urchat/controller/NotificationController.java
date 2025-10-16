package com.exotech.urchat.controller;

import com.exotech.urchat.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/token")
    public ResponseEntity<String> saveFcmToken(
            @AuthenticationPrincipal String username,
            @RequestBody Map<String, String> request) {

        String fcmToken = request.get("fcmToken");
        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("FCM token is required");
        }

        notificationService.saveFcmToken(username, fcmToken);
        return ResponseEntity.ok("FCM token saved successfully");
    }

    @DeleteMapping("/token")
    public ResponseEntity<String> removeFcmToken(@AuthenticationPrincipal String username) {
        notificationService.saveFcmToken(username, null);
        return ResponseEntity.ok("FCM token removed successfully");
    }
}