package com.exotech.urchat.controller;

import com.exotech.urchat.dto.userDTOs.UserDTO;
import com.exotech.urchat.dto.userDTOs.UserPersonalDTO;
import com.exotech.urchat.dto.userDTOs.UserSearchDTO;
import com.exotech.urchat.model.User;
import com.exotech.urchat.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchDTO>> searchUsers(
            @RequestParam String q,
            @AuthenticationPrincipal String currentUser) {
        List<UserSearchDTO> users = userService.searchUsers(q);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{username}/profile")
    public ResponseEntity<UserDTO> getUserProfile(@PathVariable String username) {
        UserDTO user = userService.getUserProfile(username);
        return ResponseEntity.ok(user);
    }



    @GetMapping("/self/profile")
    public ResponseEntity<UserPersonalDTO> getCurrentUserProfile(@AuthenticationPrincipal String username) {
        UserPersonalDTO user = userService.getCurrentUserProfile(username);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDTO> updateProfile(
            @AuthenticationPrincipal String username,
            @RequestBody User userUpdate) {
        User updatedUser = userService.updateUserProfile(username, userUpdate);
        UserDTO userDTO = userService.getUserProfile(updatedUser.getUsername());
        return ResponseEntity.ok(userDTO);
    }
}