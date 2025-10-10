package com.exotech.urchat.service;

import com.exotech.urchat.dto.messageDTOs.MessageDTO;
import com.exotech.urchat.dto.messageDTOs.MessageDTOConvertor;
import com.exotech.urchat.dto.chatDTOs.*;
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

        return savedMessage;
    }

    public List<MessageDTO> getChatMessages(String chatId, String username) {
        if (!chatRoomRepo.existsByChatIdAndParticipantUsername(chatId, username)) {
            throw new RuntimeException("Access denied to this chat");
        }
//        List<Message> messages = messageRepo.findMessagesWithChatRoom(chatId);
         List<Message> messages = messageRepo.findByChatRoom_ChatIdOrderByTimestampAsc(chatId);

        return messages.stream()
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

        return messages.stream()
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

        messageRepo.delete(message);
        log.info("Message {} deleted by user {}", messageId, username);

        // Update last message if needed
        updateChatLastMessageIfNeeded(chatId, messageId);
    }

    @Transactional
    public void deleteChat(String chatId, String username) {
        ChatRoom chatRoom = chatRoomRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        // Check if user is participant
        if (!chatRoom.hasParticipant(username)) {
            throw new RuntimeException("You are not a participant of this chat");
        }

        // For individual chats, just remove the user
        if (!chatRoom.getIsGroup()) {
            removeChatFromUser(username, chatId);
            log.info("User {} removed from individual chat {}", username, chatId);
        }
        // For group chats, only admin can delete the entire group
        else {
            if (chatRoom.getAdmin() == null || !chatRoom.getAdmin().getUsername().equals(username)) {
                throw new RuntimeException("Only group admin can delete the group");
            }

            // Delete all messages first
            messageRepo.deleteAllByChatId(chatId);

            // Then delete the chat room
            chatRoomRepo.delete(chatRoom);
            log.info("Group chat {} deleted by admin {}", chatId, username);
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
}