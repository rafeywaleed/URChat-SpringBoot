package com.exotech.urchat.service;

import com.exotech.urchat.model.ChatRoom;
import com.exotech.urchat.model.Message;
import com.exotech.urchat.repository.ChatRoomRepo;
import com.exotech.urchat.repository.MessageRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageCleanupService {

    private final MessageRepo messageRepo;
    private final ChatRoomRepo chatRoomRepo;
    private final SimpMessagingTemplate messagingTemplate;

    // Run every day at 3 AM
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void deleteMessagesOlderThan30Days() {
        log.info("Starting 30-day message cleanup...");

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        int deletedCount = 0;

        try {
            // Find messages older than 30 days
            List<Message> oldMessages = messageRepo.findMessagesOlderThan(thirtyDaysAgo);

            // Group by chat room to handle last message updates efficiently
            Map<String, List<Long>> chatMessageMap = new HashMap<>();

            for (Message message : oldMessages) {
                String chatId = message.getChatRoom().getChatId();
                chatMessageMap.computeIfAbsent(chatId, k -> new ArrayList<>()).add(message.getMessageId());
            }

            // Delete all old messages
            deletedCount = messageRepo.deleteMessagesOlderThan(thirtyDaysAgo);

            // Update last messages for affected chats
            for (String chatId : chatMessageMap.keySet()) {
                updateChatLastMessage(chatId);
            }

            log.info("✅ Deleted {} messages older than 30 days", deletedCount);

        } catch (Exception e) {
            log.error("❌ Error during message cleanup: {}", e.getMessage());
        }
    }

    private void updateChatLastMessage(String chatId) {
        try {
            ChatRoom chatRoom = chatRoomRepo.findById(chatId).orElse(null);
            if (chatRoom == null) return;

            // Find the newest message in the chat (after deletion)
            Message lastMessage = messageRepo.findLastMessageByChatId(chatId);

            if (lastMessage != null) {
                chatRoom.setLastMessage(lastMessage.getMessageContent());
                chatRoom.setLastActivity(lastMessage.getTimestamp());
            } else {
                // No messages left in the chat
                chatRoom.setLastMessage("No messages yet");
                chatRoom.setLastActivity(LocalDateTime.now());
            }

            chatRoomRepo.save(chatRoom);
            log.debug("Updated last message for chat: {}", chatId);

        } catch (Exception e) {
            log.error("Error updating last message for chat {}: {}", chatId, e.getMessage());
        }
    }

    // Optional: Cleanup empty chats (no messages and no participants)
    @Scheduled(cron = "0 0 4 * * ?") // Run at 4 AM, after message cleanup
    @Transactional
    public void cleanupEmptyChats() {
        log.info("Starting empty chats cleanup...");

        List<ChatRoom> allChats = chatRoomRepo.findAll();
        int deletedChatCount = 0;

        for (ChatRoom chat : allChats) {
            // Check if chat has no messages and no participants
            long messageCount = messageRepo.countByChatRoomChatId(chat.getChatId());
            boolean hasParticipants = !chat.getParticipants().isEmpty();

            if (messageCount == 0 && !hasParticipants) {
                chatRoomRepo.delete(chat);
                deletedChatCount++;
                log.debug("Deleted empty chat: {}", chat.getChatId());
            }
        }

        log.info("✅ Deleted {} empty chats", deletedChatCount);
    }

    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM for stats
    public void logMessageStatistics() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        long totalMessages = messageRepo.count();
        long oldMessages = messageRepo.countMessagesOlderThan(thirtyDaysAgo);
        long recentMessages = totalMessages - oldMessages;

        log.info("📊 Message Statistics:");
        log.info("   Total messages: {}", totalMessages);
        log.info("   Messages older than 30 days: {}", oldMessages);
        log.info("   Recent messages (last 30 days): {}", recentMessages);

        if (oldMessages > 0) {
            double percentage = (double) oldMessages / totalMessages * 100;
            log.info("   {}% of messages will be deleted in next cleanup", String.format("%.2f", percentage));
        }
    }

    private long countMessagesOlderThan(LocalDateTime threshold) {
        return messageRepo.countMessagesOlderThan(threshold);
    }
}