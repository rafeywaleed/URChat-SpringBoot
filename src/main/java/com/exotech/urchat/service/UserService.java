package com.exotech.urchat.service;

import com.exotech.urchat.dto.userDTOs.UserDTO;
import com.exotech.urchat.dto.userDTOs.UserDTOConvertor;
import com.exotech.urchat.dto.userDTOs.UserPersonalDTO;
import com.exotech.urchat.dto.userDTOs.UserSearchDTO;
import com.exotech.urchat.model.ChatRoom;
import com.exotech.urchat.model.User;
import com.exotech.urchat.repository.ChatRoomRepo;
import com.exotech.urchat.repository.UserRepo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final UserDTOConvertor userDTOConvertor;
    private final ChatRoomRepo chatRoomRepo;

    @Transactional
    public User createUser(User user){
//        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepo.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    public boolean existsByUsername(String username) {
        return userRepo.existsByUsername(username);
    }

    public boolean existsByEmail(String email){
        return userRepo.existsByEmail(email);
    }

    public List<UserSearchDTO> searchUsers(String key) {
        if (key.length() < 2) {
            throw new RuntimeException("Search key must be at least 2 characters long");
        }
        return userRepo.findByUsernameStartingWith(key.toLowerCase())
                .stream()
                .map(userDTOConvertor::convertToSearchDTO)
                .toList();
    }

    public UserPersonalDTO getCurrentUserProfile(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userDTOConvertor.convertToUserPersonalDTO(user);
    }

    public UserDTO getUserProfile(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userDTOConvertor.convertToUserDTO(user);
    }

    @Transactional
    public User updateUserProfile(String username, User userUpdate) {
        User existingUser = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (userUpdate.getFullName() != null) {
            existingUser.setFullName(userUpdate.getFullName());
        }
        if (userUpdate.getBio() != null) {
            existingUser.setBio(userUpdate.getBio());
        }
        if (userUpdate.getPfpIndex() != null) {
            existingUser.setPfpIndex(userUpdate.getPfpIndex());
        }
        if (userUpdate.getPfpBg() != null) {
            existingUser.setPfpBg(userUpdate.getPfpBg());
        }

        return userRepo.save(existingUser);
    }

    @Transactional
    public void addGroupInvitation(String username, String chatId) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatRoom chat = chatRoomRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getChatRooms().contains(chat)) {
            throw new RuntimeException("Already in Group");
        }

        if (!user.getChatRoomInvitations().contains(chat)) {
            user.getChatRoomInvitations().add(chat);
            userRepo.save(user);
        }
    }

    @Transactional
    public void removeGroupInvitation(String username, String chatId) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.getChatRoomInvitations().remove(chatId);
        userRepo.save(user);
    }

    public List<User> getUsersByUsernames(List<String> usernames) {
        return userRepo.findAllByUsernames(usernames);
    }
}
