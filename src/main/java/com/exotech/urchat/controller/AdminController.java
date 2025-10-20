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

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            long totalUsers = userRepo.count();
            long totalChatRooms = chatRoomRepo.count();
            long totalMessages = messageRepo.count();
            long groupChats = chatRoomRepo.findAll().stream().filter(ChatRoom::getIsGroup).count();
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

    @GetMapping("/tables/users")
    public ResponseEntity<List<Map<String, String>>> getAllUsersTable() {
        try {
            List<Map<String, String>> users = userRepo.findAll().stream()
                    .map(this::convertUserToTableRow)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error fetching users table: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/tables/chatrooms")
    public ResponseEntity<List<Map<String, String>>> getAllChatRoomsTable() {
        try {
            List<Map<String, String>> chatRooms = chatRoomRepo.findAll().stream()
                    .map(this::convertChatRoomToTableRow)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(chatRooms);
        } catch (Exception e) {
            log.error("Error fetching chat rooms table: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/tables/messages")
    public ResponseEntity<List<Map<String, String>>> getAllMessagesTable() {
        try {
            List<Map<String, String>> messages = messageRepo.findAll().stream()
                    .map(this::convertMessageToTableRow)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error fetching messages table: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/tables/chatroom-participants")
    public ResponseEntity<List<Map<String, String>>> getChatRoomParticipantsTable() {
        try {
            List<Map<String, String>> relationships = chatRoomRepo.findAll().stream()
                    .flatMap(chatRoom -> chatRoom.getParticipants().stream()
                            .map(user -> {
                                Map<String, String> rel = new HashMap<>();
                                rel.put("chatId", chatRoom.getChatId());
                                rel.put("chatName", chatRoom.getChatName() != null ? chatRoom.getChatName() : "N/A");
                                rel.put("username", user.getUsername());
                                rel.put("isGroup", String.valueOf(chatRoom.getIsGroup()));
                                rel.put("userEmail", user.getEmail());
                                rel.put("userFullName", user.getFullName());
                                return rel;
                            }))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(relationships);
        } catch (Exception e) {
            log.error("Error fetching chat room participants table: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/tables/chatroom-invitations")
    public ResponseEntity<List<Map<String, String>>> getChatRoomInvitationsTable() {
        try {
            List<Map<String, String>> relationships = chatRoomRepo.findAll().stream()
                    .flatMap(chatRoom -> chatRoom.getPendingInvitations().stream()
                            .map(user -> {
                                Map<String, String> rel = new HashMap<>();
                                rel.put("chatId", chatRoom.getChatId());
                                rel.put("chatName", chatRoom.getChatName() != null ? chatRoom.getChatName() : "N/A");
                                rel.put("username", user.getUsername());
                                rel.put("isGroup", String.valueOf(chatRoom.getIsGroup()));
                                rel.put("userEmail", user.getEmail());
                                rel.put("userFullName", user.getFullName());
                                return rel;
                            }))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(relationships);
        } catch (Exception e) {
            log.error("Error fetching chat room invitations table: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/users/{username}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable String username) {
        try {
            User user = userRepo.findById(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            user.getChatRooms().forEach(chat -> chat.getParticipants().remove(user));
            user.getChatRoomInvitations().forEach(chat -> chat.getPendingInvitations().remove(user));

            chatRoomRepo.findAll().forEach(chat -> {
                if (chat.getAdmin() != null && chat.getAdmin().getUsername().equals(username)) {
                    chat.setAdmin(null);
                }
            });

            userRepo.delete(user);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "User deleted successfully: " + username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting user: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/chatrooms/{chatId}")
    public ResponseEntity<Map<String, String>> deleteChatRoom(@PathVariable String chatId) {
        try {
            ChatRoom chatRoom = chatRoomRepo.findById(chatId)
                    .orElseThrow(() -> new RuntimeException("Chat room not found: " + chatId));
            chatRoomRepo.delete(chatRoom);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Chat room deleted successfully: " + chatId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting chat room: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Map<String, String>> deleteMessage(@PathVariable Long messageId) {
        try {
            Message message = messageRepo.findById(messageId)
                    .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));
            messageRepo.delete(message);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Message deleted successfully: " + messageId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting message: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/users/{username}")
    public ResponseEntity<Map<String, String>> updateUser(@PathVariable String username, @RequestBody Map<String, String> updates) {
        try {
            User user = userRepo.findById(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            if (updates.containsKey("email")) user.setEmail(updates.get("email"));
            if (updates.containsKey("fullName")) user.setFullName(updates.get("fullName"));
            if (updates.containsKey("bio")) user.setBio(updates.get("bio"));
            if (updates.containsKey("pfpIndex")) user.setPfpIndex(updates.get("pfpIndex"));
            if (updates.containsKey("pfpBg")) user.setPfpBg(updates.get("pfpBg"));
            if (updates.containsKey("fcmToken")) user.setFcmToken(updates.get("fcmToken"));

            userRepo.save(user);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "User updated successfully: " + username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/chatrooms/{chatId}")
    public ResponseEntity<Map<String, String>> updateChatRoom(@PathVariable String chatId, @RequestBody Map<String, String> updates) {
        try {
            ChatRoom chatRoom = chatRoomRepo.findById(chatId)
                    .orElseThrow(() -> new RuntimeException("Chat room not found: " + chatId));

            if (updates.containsKey("chatName")) chatRoom.setChatName(updates.get("chatName"));
            if (updates.containsKey("pfpIndex")) chatRoom.setPfpIndex(updates.get("pfpIndex"));
            if (updates.containsKey("pfpBg")) chatRoom.setPfpBg(updates.get("pfpBg"));
            if (updates.containsKey("lastMessage")) chatRoom.setLastMessage(updates.get("lastMessage"));
            if (updates.containsKey("themeIndex")) chatRoom.setThemeIndex(Integer.parseInt(updates.get("themeIndex")));
            if (updates.containsKey("isDarkTheme")) chatRoom.setIsDarkTheme(Boolean.parseBoolean(updates.get("isDarkTheme")));

            chatRoomRepo.save(chatRoom);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Chat room updated successfully: " + chatId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating chat room: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/messages/{messageId}")
    public ResponseEntity<Map<String, String>> updateMessage(@PathVariable Long messageId, @RequestBody Map<String, String> updates) {
        try {
            Message message = messageRepo.findById(messageId)
                    .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

            if (updates.containsKey("messageContent")) {
                message.setMessageContent(updates.get("messageContent"));
            }

            messageRepo.save(message);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Message updated successfully: " + messageId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating message: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/relationships/chatroom-participants")
    public ResponseEntity<Map<String, String>> removeParticipant(@RequestParam String chatId, @RequestParam String username) {
        try {
            ChatRoom chatRoom = chatRoomRepo.findById(chatId)
                    .orElseThrow(() -> new RuntimeException("Chat room not found: " + chatId));
            User user = userRepo.findById(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            chatRoom.getParticipants().remove(user);
            chatRoomRepo.save(chatRoom);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Participant removed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error removing participant: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/relationships/chatroom-invitations")
    public ResponseEntity<Map<String, String>> removeInvitation(@RequestParam String chatId, @RequestParam String username) {
        try {
            ChatRoom chatRoom = chatRoomRepo.findById(chatId)
                    .orElseThrow(() -> new RuntimeException("Chat room not found: " + chatId));
            User user = userRepo.findById(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            chatRoom.getPendingInvitations().remove(user);
            chatRoomRepo.save(chatRoom);

            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Invitation removed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error removing invitation: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private Map<String, String> convertUserToTableRow(User user) {
        Map<String, String> row = new HashMap<>();
        row.put("username", user.getUsername());
        row.put("email", user.getEmail());
        row.put("password", user.getPassword());
        row.put("fullName", user.getFullName());
        row.put("bio", user.getBio() != null ? user.getBio() : "N/A");
        row.put("pfpIndex", user.getPfpIndex());
        row.put("pfpBg", user.getPfpBg());
        row.put("joinedAt", user.getJoinedAt() != null ? user.getJoinedAt().toString() : "N/A");
        row.put("fcmToken", user.getFcmToken() != null ? user.getFcmToken() : "N/A");
        row.put("refreshToken", user.getRefreshToken() != null ? user.getRefreshToken() : "N/A");
        row.put("refreshTokenExpiry", user.getRefreshTokenExpiry() != null ? user.getRefreshTokenExpiry().toString() : "N/A");
        row.put("chatRoomsCount", String.valueOf(user.getChatRooms() != null ? user.getChatRooms().size() : 0));
        row.put("invitationsCount", String.valueOf(user.getChatRoomInvitations() != null ? user.getChatRoomInvitations().size() : 0));
        return row;
    }

    private Map<String, String> convertChatRoomToTableRow(ChatRoom chatRoom) {
        Map<String, String> row = new HashMap<>();
        row.put("chatId", chatRoom.getChatId());
        row.put("chatName", chatRoom.getChatName() != null ? chatRoom.getChatName() : "N/A");
        row.put("isGroup", String.valueOf(chatRoom.getIsGroup()));
        row.put("pfpIndex", chatRoom.getPfpIndex() != null ? chatRoom.getPfpIndex() : "N/A");
        row.put("pfpBg", chatRoom.getPfpBg() != null ? chatRoom.getPfpBg() : "N/A");
        row.put("lastActivity", chatRoom.getLastActivity() != null ? chatRoom.getLastActivity().toString() : "N/A");
        row.put("lastMessage", chatRoom.getLastMessage() != null ? chatRoom.getLastMessage() : "N/A");
        row.put("themeIndex", String.valueOf(chatRoom.getThemeIndex()));
        row.put("isDarkTheme", String.valueOf(chatRoom.getIsDarkTheme()));
        row.put("admin", chatRoom.getAdmin() != null ? chatRoom.getAdmin().getUsername() : "N/A");
        row.put("participantsCount", String.valueOf(chatRoom.getParticipants() != null ? chatRoom.getParticipants().size() : 0));
        row.put("pendingInvitationsCount", String.valueOf(chatRoom.getPendingInvitations() != null ? chatRoom.getPendingInvitations().size() : 0));
        row.put("messagesCount", String.valueOf(chatRoom.getMessages() != null ? chatRoom.getMessages().size() : 0));
        return row;
    }

    private Map<String, String> convertMessageToTableRow(Message message) {
        Map<String, String> row = new HashMap<>();
        row.put("messageId", String.valueOf(message.getMessageId()));
        row.put("messageContent", message.getMessageContent() != null ? message.getMessageContent() : "N/A");
        row.put("timestamp", message.getTimestamp() != null ? message.getTimestamp().toString() : "N/A");
        row.put("senderUsername", message.getSender() != null ? message.getSender().getUsername() : "N/A");
        row.put("chatId", message.getChatRoom() != null ? message.getChatRoom().getChatId() : "N/A");
        return row;
    }
}