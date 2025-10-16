package com.exotech.urchat.controller;

import com.exotech.urchat.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/admin")
public class Hello {

    private final NotificationService notificationService;

    @PostMapping("/send-test-notification")
    public ResponseEntity<String> sendTestNotification(@AuthenticationPrincipal String username) {
        try {
            notificationService.sendMessageNotification(
                    "test-chat-id",
                    username,
                    "This is a test notification from URChat!",
                    "Test Chat",
                    false
            );
            return ResponseEntity.ok("Test notification sent successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send test notification: " + e.getMessage());
        }
    }

    @GetMapping("/hello")
    public String greet(){
        return "Heyyaaaa";
    }

}
