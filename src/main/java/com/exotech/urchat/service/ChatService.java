package com.exotech.urchat.service;

import com.exotech.urchat.dto.messageDTOs.MessageDTO;
import com.exotech.urchat.dto.messageDTOs.MessageDTOConvertor;
import com.exotech.urchat.dto.chatDTOs.*;
import com.exotech.urchat.dto.messageDTOs.MessageStatsDTO;
import com.exotech.urchat.dto.webSocketDTOs.ChatDeletionBroadcast;
import com.exotech.urchat.dto.webSocketDTOs.MessageDeletionBroadcast;
import com.exotech.urchat.model.ChatRoom;
import com.exotech.urchat.model.Message;
import com.exotech.urchat.model.User;
import com.exotech.urchat.repository.ChatRoomRepo;
import com.exotech.urchat.repository.MessageRepo;
import com.exotech.urchat.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepo chatRoomRepo;
    private final MessageRepo messageRepo;
    private final UserRepo userRepo;
    private final MessageDTOConvertor messageDTOConvertor;
    private final ChatDTOConvertor chatDTOConvertor;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    @Transactional
    public ChatRoomDTO getOrCreateIndividualChat(String user1, String user2) {
        Optional<ChatRoom> existingChat = chatRoomRepo.findIndividualChat(user1, user2);
        if (existingChat.isPresent()) {
            return chatDTOConvertor.convertToChatRoomDTO(existingChat.get());
        }

        User user1Entity = userRepo.findByUsername(user1)
                .orElseThrow(() -> new RuntimeException("User " + user1 + " not found"));
        User user2Entity = userRepo.findByUsername(user2)
                .orElseThrow(() -> new RuntimeException("User " + user2 + " not found"));

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setChatId(UUID.randomUUID().toString());
        chatRoom.setIsGroup(false);
        chatRoom.setPfpIndex("💬");
        chatRoom.setPfpBg("#2196F3");
        chatRoom.setIsDarkTheme(false);
        chatRoom.setLastActivity(LocalDateTime.now());
        chatRoom.setLastMessage("No messages yet");

        chatRoom.getParticipants().add(user1Entity);
        chatRoom.getParticipants().add(user2Entity);

        return chatDTOConvertor.convertToChatRoomDTO(chatRoomRepo.save(chatRoom));
    }

    @Transactional
    public Message sendMessage(String senderUsername, String chatId, String content) {
        ChatRoom chat = chatRoomRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        User sender = userRepo.findByUsername(senderUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isParticipant = chat.hasParticipant(senderUsername);
        if (!isParticipant) {
            throw new RuntimeException("You cannot message in this chat");
        }

        Message message = new Message();
        message.setMessageContent(content);
        message.setSender(sender);
        message.setChatRoom(chat);

        Message savedMessage = messageRepo.save(message);

        chat.setLastMessage(message.getMessageContent());
        chat.setLastActivity(LocalDateTime.now());
        chatRoomRepo.save(chat);

        String chatDisplayName = chat.getDisplayName(senderUsername);

        notificationService.sendMessageNotification(
                chatId,
                senderUsername,
                content,
                chatDisplayName,
                chat.getIsGroup()
        );

        return savedMessage;
    }

    public List<MessageDTO> getChatMessages(String chatId, String username) {
        if (!chatRoomRepo.existsByChatIdAndParticipantUsername(chatId, username)) {
            throw new RuntimeException("Access denied to this chat");
        }
//        List<Message> messages = messageRepo.findMessagesWithChatRoom(chatId);
         List<Message> messages = messageRepo.findByChatRoom_ChatIdOrderByTimestampAsc(chatId);

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(7);

        return messages.stream()
                .filter(message -> message.getTimestamp().isAfter(thirtyDaysAgo))
                .map(message -> messageDTOConvertor.convertToMessageDTO(message, username))
                .collect(Collectors.toList());
    }

    public List<MessageDTO> getPaginatedMessages(String chatId, int page, int size, String currentUser) {
        if (!chatRoomRepo.existsByChatIdAndParticipantUsername(chatId, currentUser)) {
            throw new RuntimeException("Access denied to this chat");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        List<Message> messages = messageRepo.findByChatRoomChatIdOrderByTimestampDesc(chatId, pageable);
        Collections.reverse(messages);

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(7);

        return messages.stream()
                .filter(message -> message.getTimestamp().isAfter(thirtyDaysAgo))
                .map(message -> messageDTOConvertor.convertToMessageDTO(message, currentUser))
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateChatListsForParticipants(String chatId, String username) {
        try {
            ChatRoom chatRoom = chatRoomRepo.findByIdWithParticipants(chatId)
                    .orElseThrow(() -> new RuntimeException("Chat room not found"));

            log.info("🚀 Updating chat lists for {} participants in chat: {}",
                    chatRoom.getParticipants().size(), chatId);

            // Update chat list for all participants
            for (User participant : chatRoom.getParticipants()) {
                String participantUsername = participant.getUsername();
                updateChatListForUser(participantUsername);
                log.info("   📤 Sent update to: {}", participantUsername);
            }

            log.info("✅ Successfully updated chat lists for all participants");

        } catch (Exception e) {
            log.error("❌ Error updating chat lists: {}", e.getMessage());
            throw new RuntimeException("Failed to update chat lists", e);
        }
    }

    private void updateChatListForUser(String username) {
        try {
            List<ChatRoomDTO> updatedChats = getUserChats(username);

            log.info("📤 Sending chat list update to user: {} with {} chats", username, updatedChats.size());

            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/chats/update",
                    updatedChats
            );

            log.debug("✅ Sent chat list update to user: {}", username);
        } catch (Exception e) {
            log.error("❌ Error updating chat list for user {}: {}", username, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<ChatRoomDTO> getUserChats(String username) {
        System.out.println("Entered Get Users Chat Service");
        List<ChatRoom> chats = chatRoomRepo.findByParticipantUsername(username);
        chats.sort((a,b) -> b.getLastActivity().compareTo(a.getLastActivity()));

        return chats.stream().map(chat -> {
            ChatRoomDTO dto = chatDTOConvertor.convertToChatRoomDTO(chat);
            dto.setChatName(chat.getDisplayName(username));
            dto.setPfpIndex(chat.getChatPfpIndex(username));
            dto.setPfpBg(chat.getChatPfpBg(username));
            dto.setLastMessage(chat.getLastMessage());

            System.out.println(chat.getDisplayName(username) + "  " + chat.getChatPfpIndex(username) + "  " + chat.getChatPfpBg(username));

//            Message lastMessage = messageRepo.findLastMessageByChatId(chat.getChatId());
//            if (lastMessage != null) {
//                dto.setLastMessage(lastMessage.getMessageContent());
//            }
            return dto;
        }).collect(Collectors.toList());
    }

    public ChatRoom getChatRoom(String chatId) {
        return chatRoomRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
    }

    @Transactional
    public void updateChatLastActivity(String chatId) {
        ChatRoom chat = chatRoomRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        chat.setLastActivity(LocalDateTime.now());
        chatRoomRepo.save(chat);
    }
    
    @Transactional
    public void addChatToUser(String username, String chatId) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User " + username + " not found"));
        ChatRoom chat = chatRoomRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        if (!chat.hasParticipant(username)) {
            chat.getParticipants().add(user);
            chatRoomRepo.save(chat);
        }
    }

    @Transactional
    public void removeChatFromUser(String username, String chatId) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        ChatRoom chat = chatRoomRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        chat.getParticipants().removeIf(participant -> participant.getUsername().equals(username));
        chatRoomRepo.save(chat);
    }

    @Transactional
    public void removeChatFromAllUsers(String chatId) {
        ChatRoom chat = chatRoomRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        chat.getParticipants().clear();
        chat.getPendingInvitations().clear();
        chatRoomRepo.save(chat);
    }

    public ChatThemeDTO getTheme(String chatId) {
        ChatRoom chat = chatRoomRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        return new ChatThemeDTO(chat.getThemeIndex(), chat.getIsDarkTheme());
    }

    public ChatThemeDTO setTheme(String chatId, ChatThemeDTO newTheme) {
        ChatRoom chat = chatRoomRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));
        chat.setThemeIndex(newTheme.getThemeIndex());
        chat.setIsDarkTheme(newTheme.getIsDark());
        chatRoomRepo.save(chat);
        return new ChatThemeDTO(chat.getThemeIndex(), chat.getIsDarkTheme());
    }

    @Transactional
    public void deleteMessage(Long messageId, String username) {
        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Check if the user is the sender of the message
        if (!message.getSender().getUsername().equals(username)) {
            throw new RuntimeException("You can only delete your own messages");
        }

        // Check if user is participant in the chat
        String chatId = message.getChatRoom().getChatId();
        if (!chatRoomRepo.existsByChatIdAndParticipantUsername(chatId, username)) {
            throw new RuntimeException("Access denied to this chat");
        }

        // PERMANENTLY DELETE THE MESSAGE FROM DATABASE
        messageRepo.delete(message);
        log.info("Message {} PERMANENTLY deleted by user {} from database", messageId, username);

        // Update last message if needed
        updateChatLastMessageIfNeeded(chatId, messageId);

        // Broadcast deletion to all participants
        broadcastMessageDeletion(chatId, messageId);
    }

    private void broadcastMessageDeletion(String chatId, Long messageId) {
        try {
            MessageDeletionBroadcast broadcast = new MessageDeletionBroadcast(messageId, chatId);
            messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/message-deleted", broadcast);
            log.info("Broadcasted message deletion {} to all participants of chat {}", messageId, chatId);
        } catch (Exception e) {
            log.error("Error broadcasting message deletion: {}", e.getMessage());
        }
    }

    @Transactional
    public void deleteChat(String chatId, String username) {
        ChatRoom chatRoom = chatRoomRepo.findByIdWithParticipants(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        // Check if user is participant
        if (!chatRoom.hasParticipant(username)) {
            throw new RuntimeException("You are not a participant of this chat");
        }

        // For individual chats - PERMANENTLY DELETE FOR BOTH USERS
        if (!chatRoom.getIsGroup()) {
            permanentlyDeleteIndividualChat(chatId, username);
        }
        // For group chats, only admin can delete the entire group
        else {
            if (chatRoom.getAdmin() == null || !chatRoom.getAdmin().getUsername().equals(username)) {
                throw new RuntimeException("Only group admin can delete the group");
            }

            // Admin is deleting the entire group - PERMANENTLY DELETE EVERYTHING
            permanentlyDeleteGroupWithAllMessages(chatId, username);
        }
    }

    @Transactional
    public void permanentlyDeleteGroupWithAllMessages(String chatId, String deletedBy) {
        try {
            ChatRoom chat = chatRoomRepo.findById(chatId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));

            if (!chat.getIsGroup()) {
                throw new RuntimeException("Chat is not a group");
            }

            log.info("🗑️ Admin {} initiating permanent deletion of group {} and ALL messages", deletedBy, chatId);

            // Get participant list for notification before deletion
            List<String> participantUsernames = chat.getParticipants().stream()
                    .map(User::getUsername)
                    .collect(Collectors.toList());

            // 1. Delete all messages from the group
            int deletedMessages = messageRepo.deleteAllByChatId(chatId);
            log.info("✅ Deleted {} messages from group {}", deletedMessages, chatId);

            // 2. Delete the group itself from database
            chatRoomRepo.delete(chat);
            log.info("✅ PERMANENTLY deleted group {} from database by admin {}", chatId, deletedBy);

            // 3. Broadcast group deletion to all former participants
            broadcastGroupDeletionToParticipants(chatId, participantUsernames, deletedBy);

            // 4. Verify deletion
            verifyGroupDeletion(chatId);

        } catch (Exception e) {
            log.error("❌ Error in admin group deletion {}: {}", chatId, e.getMessage());
            throw new RuntimeException("Failed to delete group: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void permanentlyDeleteIndividualChat(String chatId, String deletedBy) {
        try {
            ChatRoom chat = chatRoomRepo.findById(chatId)
                    .orElseThrow(() -> new RuntimeException("Chat not found"));

            if (chat.getIsGroup()) {
                throw new RuntimeException("Chat is a group, not individual");
            }

            log.info("🗑️ User {} initiating permanent deletion of individual chat {}", deletedBy, chatId);

            // Get participant list for notification before deletion
            List<String> participantUsernames = chat.getParticipants().stream()
                    .map(User::getUsername)
                    .collect(Collectors.toList());

            // 1. Delete all messages from the chat
            int deletedMessages = messageRepo.deleteAllByChatId(chatId);
            log.info("✅ Deleted {} messages from individual chat {}", deletedMessages, chatId);

            // 2. Delete the chat itself from database
            chatRoomRepo.delete(chat);
            log.info("✅ PERMANENTLY deleted individual chat {} from database by user {}", chatId, deletedBy);

            // 3. Broadcast chat deletion to both participants
            broadcastChatDeletionToParticipants(chatId, participantUsernames, deletedBy);

            // 4. Verify deletion
            verifyChatDeletion(chatId);

        } catch (Exception e) {
            log.error("❌ Error in individual chat deletion {}: {}", chatId, e.getMessage());
            throw new RuntimeException("Failed to delete chat: " + e.getMessage(), e);
        }
    }

    private void broadcastGroupDeletionToParticipants(String chatId, List<String> participantUsernames, String deletedBy) {
        try {
            ChatDeletionBroadcast broadcast = new ChatDeletionBroadcast(chatId, deletedBy, "admin_deleted");

            // Send to each participant individually
            for (String username : participantUsernames) {
                messagingTemplate.convertAndSendToUser(
                        username,
                        "/queue/chat-deleted",
                        broadcast
                );
            }

            // Also broadcast to the chat topic for real-time updates
            messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/chat-deleted", broadcast);

            log.info("📢 Broadcasted group deletion {} to {} participants", chatId, participantUsernames.size());
        } catch (Exception e) {
            log.error("Error broadcasting group deletion: {}", e.getMessage());
        }
    }

    private void verifyChatDeletion(String chatId) {
        boolean chatExists = chatRoomRepo.existsById(chatId);
        long messageCount = messageRepo.countByChatRoomChatId(chatId);

        if (!chatExists && messageCount == 0) {
            log.info("✅ VERIFIED: Chat {} and all messages successfully deleted from database", chatId);
        } else {
            log.error("❌ VERIFICATION FAILED: Chat {} still exists: {}, remaining messages: {}",
                    chatId, chatExists, messageCount);
            throw new RuntimeException("Chat deletion verification failed");
        }
    }

    private void verifyGroupDeletion(String chatId) {
        boolean groupExists = chatRoomRepo.existsById(chatId);
        long remainingMessages = messageRepo.countByChatRoomChatId(chatId);

        if (!groupExists && remainingMessages == 0) {
            log.info("✅ VERIFIED: Group {} and all messages successfully deleted from database", chatId);
        } else {
            log.error("❌ VERIFICATION FAILED: Group {} still exists: {}, remaining messages: {}",
                    chatId, groupExists, remainingMessages);
            throw new RuntimeException("Group deletion verification failed");
        }
    }

    private void broadcastChatDeletionToParticipants(String chatId, List<String> participantUsernames, String deletedBy) {
        try {
            ChatDeletionBroadcast broadcast = new ChatDeletionBroadcast(chatId, deletedBy, "user_deleted");

            // Send to each participant individually
            for (String username : participantUsernames) {
                messagingTemplate.convertAndSendToUser(
                        username,
                        "/queue/chat-deleted",
                        broadcast
                );
            }

            // Also broadcast to the chat topic for real-time updates
            messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/chat-deleted", broadcast);

            log.info("📢 Broadcasted individual chat deletion {} to {} participants", chatId, participantUsernames.size());
        } catch (Exception e) {
            log.error("Error broadcasting individual chat deletion: {}", e.getMessage());
        }
    }

    private void broadcastChatDeletion(String chatId) {
        try {
            // This will notify all users that the chat no longer exists
            // Frontend should handle this by removing the chat from their lists
            messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/chat-deleted", chatId);
            log.info("Broadcasted chat deletion {} to all participants", chatId);
        } catch (Exception e) {
            log.error("Error broadcasting chat deletion: {}", e.getMessage());
        }
    }

    private void updateChatLastMessageIfNeeded(String chatId, Long deletedMessageId) {
        ChatRoom chatRoom = chatRoomRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        // If the deleted message was the last message, update with the new last message
        Message lastMessage = messageRepo.findLastMessageByChatId(chatId);
        if (lastMessage != null) {
            chatRoom.setLastMessage(lastMessage.getMessageContent());
            chatRoom.setLastActivity(lastMessage.getTimestamp());
        } else {
            chatRoom.setLastMessage("No messages yet");
            chatRoom.setLastActivity(LocalDateTime.now());
        }
        chatRoomRepo.save(chatRoom);
    }

    @Transactional
    public void leaveGroup(String chatId, String username) {
        ChatRoom chatRoom = chatRoomRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        if (!chatRoom.getIsGroup()) {
            throw new RuntimeException("This is not a group chat");
        }

        if (!chatRoom.hasParticipant(username)) {
            throw new RuntimeException("You are not a participant of this group");
        }

        // Remove user from participants
        chatRoom.getParticipants().removeIf(user -> user.getUsername().equals(username));
        chatRoomRepo.save(chatRoom);

        log.info("User {} left group {}", username, chatId);

        // Update chat lists for remaining participants
        updateChatListsForParticipants(chatId, username);
    }

    public MessageStatsDTO getMessageStats(String chatId, String username) {
        if (!chatRoomRepo.existsByChatIdAndParticipantUsername(chatId, username)) {
            throw new RuntimeException("Access denied to this chat");
        }

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(7);
        long totalMessages = messageRepo.countByChatRoomChatId(chatId);
        long recentMessages = messageRepo.countByChatRoomChatIdAndTimestampAfter(chatId, thirtyDaysAgo);
        long oldMessages = totalMessages - recentMessages;

        return new MessageStatsDTO(totalMessages, recentMessages, oldMessages);
    }
}