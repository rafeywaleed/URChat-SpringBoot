// File: AdminController.java
package com.exotech.urchat.controller;

import com.exotech.urchat.dto.chatDTOs.ChatRoomDTO;
import com.exotech.urchat.dto.messageDTOs.MessageDTO;
import com.exotech.urchat.dto.userDTOs.UserDTO;
import com.exotech.urchat.model.ChatRoom;
import com.exotech.urchat.model.Message;
import com.exotech.urchat.model.User;
import com.exotech.urchat.repository.ChatRoomRepo;
import com.exotech.urchat.repository.MessageRepo;
import com.exotech.urchat.repository.UserRepo;
import com.exotech.urchat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("admin/${ADMIN_URL}")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepo userRepo;
    private final ChatRoomRepo chatRoomRepo;
    private final MessageRepo messageRepo;
    private final ChatService chatService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            long totalUsers = userRepo.count();
            long totalChatRooms = chatRoomRepo.count();
            long totalMessages = messageRepo.count();

            // Count group vs individual chats
            long groupChats = chatRoomRepo.findAll().stream()
                    .filter(ChatRoom::getIsGroup)
                    .count();
            long individualChats = totalChatRooms - groupChats;

            stats.put("totalUsers", totalUsers);
            stats.put("totalChatRooms", totalChatRooms);
            stats.put("totalMessages", totalMessages);
            stats.put("groupChats", groupChats);
            stats.put("individualChats", individualChats);
            stats.put("status", "success");

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Error fetching dashboard stats: {}", e.getMessage());
            stats.put("status", "error");
            stats.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(stats);
        }
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        try {
            List<Map<String, Object>> users = userRepo.findAll().stream()
                    .map(this::convertUserToMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error fetching users: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/chatrooms")
    public ResponseEntity<List<Map<String, Object>>> getAllChatRooms() {
        try {
            List<Map<String, Object>> chatRooms = chatRoomRepo.findAll().stream()
                    .map(this::convertChatRoomToMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(chatRooms);
        } catch (Exception e) {
            log.error("Error fetching chat rooms: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/messages")
    public ResponseEntity<List<Map<String, Object>>> getAllMessages() {
        try {
            List<Map<String, Object>> messages = messageRepo.findAll().stream()
                    .map(this::convertMessageToMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error fetching messages: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/relationships/chatroom-participants")
    public ResponseEntity<List<Map<String, Object>>> getChatRoomParticipants() {
        try {
            List<Map<String, Object>> relationships = chatRoomRepo.findAll().stream()
                    .flatMap(chatRoom -> chatRoom.getParticipants().stream()
                            .map(user -> {
                                Map<String, Object> rel = new HashMap<>();
                                rel.put("chatId", chatRoom.getChatId());
                                rel.put("chatName", chatRoom.getChatName());
                                rel.put("username", user.getUsername());
                                rel.put("isGroup", chatRoom.getIsGroup());
                                return rel;
                            }))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(relationships);
        } catch (Exception e) {
            log.error("Error fetching chat room participants: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/relationships/chatroom-invitations")
    public ResponseEntity<List<Map<String, Object>>> getChatRoomInvitations() {
        try {
            List<Map<String, Object>> relationships = chatRoomRepo.findAll().stream()
                    .flatMap(chatRoom -> chatRoom.getPendingInvitations().stream()
                            .map(user -> {
                                Map<String, Object> rel = new HashMap<>();
                                rel.put("chatId", chatRoom.getChatId());
                                rel.put("chatName", chatRoom.getChatName());
                                rel.put("username", user.getUsername());
                                rel.put("isGroup", chatRoom.getIsGroup());
                                return rel;
                            }))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(relationships);
        } catch (Exception e) {
            log.error("Error fetching chat room invitations: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private Map<String, Object> convertUserToMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail());
        userMap.put("fullName", user.getFullName());
        userMap.put("bio", user.getBio());
        userMap.put("pfpIndex", user.getPfpIndex());
        userMap.put("pfpBg", user.getPfpBg());
        userMap.put("joinedAt", user.getJoinedAt());
        userMap.put("fcmToken", user.getFcmToken());
        userMap.put("chatRoomsCount", user.getChatRooms() != null ? user.getChatRooms().size() : 0);
        userMap.put("invitationsCount", user.getChatRoomInvitations() != null ? user.getChatRoomInvitations().size() : 0);
        return userMap;
    }

    private Map<String, Object> convertChatRoomToMap(ChatRoom chatRoom) {
        Map<String, Object> chatMap = new HashMap<>();
        chatMap.put("chatId", chatRoom.getChatId());
        chatMap.put("chatName", chatRoom.getChatName());
        chatMap.put("isGroup", chatRoom.getIsGroup());
        chatMap.put("pfpIndex", chatRoom.getPfpIndex());
        chatMap.put("pfpBg", chatRoom.getPfpBg());
        chatMap.put("lastActivity", chatRoom.getLastActivity());
        chatMap.put("lastMessage", chatRoom.getLastMessage());
        chatMap.put("themeIndex", chatRoom.getThemeIndex());
        chatMap.put("isDarkTheme", chatRoom.getIsDarkTheme());
        chatMap.put("participantsCount", chatRoom.getParticipants() != null ? chatRoom.getParticipants().size() : 0);
        chatMap.put("pendingInvitationsCount", chatRoom.getPendingInvitations() != null ? chatRoom.getPendingInvitations().size() : 0);
        chatMap.put("messagesCount", chatRoom.getMessages() != null ? chatRoom.getMessages().size() : 0);
        chatMap.put("admin", chatRoom.getAdmin() != null ? chatRoom.getAdmin().getUsername() : null);
        return chatMap;
    }

    private Map<String, Object> convertMessageToMap(Message message) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("messageId", message.getMessageId());
        messageMap.put("messageContent", message.getMessageContent());
        messageMap.put("timestamp", message.getTimestamp());
        messageMap.put("senderUsername", message.getSender() != null ? message.getSender().getUsername() : null);
        messageMap.put("chatId", message.getChatRoom() != null ? message.getChatRoom().getChatId() : null);
        return messageMap;
    }
}