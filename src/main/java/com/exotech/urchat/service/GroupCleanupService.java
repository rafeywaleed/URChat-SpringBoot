package com.exotech.urchat.service;

import com.exotech.urchat.model.ChatRoom;
import com.exotech.urchat.repository.ChatRoomRepo;
import com.exotech.urchat.repository.MessageRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GroupCleanupService {

    private final ChatRoomRepo chatRoomRepo;
    private final MessageRepo messageRepo;
    private final SimpMessagingTemplate messagingTemplate;

    // Run every hour to check for empty groups
    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void cleanupEmptyGroups() {
        log.info("🔄 Starting scheduled empty groups cleanup...");

        List<ChatRoom> allGroups = chatRoomRepo.findAll().stream()
                .filter(ChatRoom::getIsGroup)
                .collect(Collectors.toList());

        int deletedCount = 0;

        for (ChatRoom group : allGroups) {
            if (group.getParticipants().isEmpty()) {
                log.info("🗑️ Found empty group: {} ({}). Deleting...",
                        group.getChatName(), group.getChatId());

                try {
                    // Delete all messages
                    int deletedMessages = messageRepo.deleteAllByChatId(group.getChatId());

                    // Delete the group
                    chatRoomRepo.delete(group);

                    log.info("✅ Deleted empty group {} with {} messages",
                            group.getChatId(), deletedMessages);
                    deletedCount++;

                } catch (Exception e) {
                    log.error("❌ Failed to delete empty group {}: {}", group.getChatId(), e.getMessage());
                }
            }
        }

        log.info("✅ Empty groups cleanup completed. Deleted {} empty groups", deletedCount);
    }

    // Run daily to check for groups with only 1 participant
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @Transactional
    public void cleanupAlmostEmptyGroups() {
        log.info("🔄 Starting scheduled almost-empty groups cleanup...");

        List<ChatRoom> allGroups = chatRoomRepo.findAll().stream()
                .filter(ChatRoom::getIsGroup)
                .collect(Collectors.toList());

        int notifiedCount = 0;

        for (ChatRoom group : allGroups) {
            if (group.getParticipants().size() == 1) {
                log.info("⚠️  Group {} has only 1 participant. Notifying admin...", group.getChatName());

                // You could send a notification to the admin here
                // For now, just log it
                notifiedCount++;
            }
        }

        log.info("✅ Almost-empty groups check completed. Found {} groups with 1 participant", notifiedCount);
    }
}