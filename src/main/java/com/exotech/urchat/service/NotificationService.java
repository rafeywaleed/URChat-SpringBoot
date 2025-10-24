package com.exotech.urchat.service;

import com.exotech.urchat.model.ChatRoom;
import com.exotech.urchat.model.User;
import com.exotech.urchat.repository.ChatRoomRepo;
import com.exotech.urchat.repository.UserRepo;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final FirebaseMessaging firebaseMessaging;
    private final UserRepo userRepo;
    private final ChatRoomRepo chatRoomRepo;

    public void saveFcmToken(String username, String fcmToken) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setFcmToken(fcmToken);
        userRepo.save(user);
        log.info("✅ FCM token saved for user: {}", username);
    }

    public void sendMessageNotification(String chatId, String senderUsername,
                                        String messageContent, String chatName, boolean isGroup) {
        try {
            ChatRoom chatRoom = chatRoomRepo.findById(chatId)
                    .orElseThrow(() -> new RuntimeException("Chat room not found"));

            if (!isGroup) {

                sendDMNotification(chatId, senderUsername, messageContent, chatName, chatRoom);
            } else {

                sendGroupNotification(chatId, senderUsername, messageContent, chatName, chatRoom);
            }
        } catch (Exception e) {
            log.error("❌ Error sending notification: {}", e.getMessage());
        }
    }

    private void sendDMNotification(String chatId, String senderUsername,
                                    String messageContent, String chatName, ChatRoom chatRoom) {
        try {
            User recipient = chatRoom.getParticipants().stream()
                    .filter(user -> !user.getUsername().equals(senderUsername))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Recipient not found in DM"));

            if (recipient.getUsername().equals(senderUsername)) {
                log.info("🔕 Skipping self-notification for user: {}", senderUsername);
                return;
            }

            if (recipient.getFcmToken() != null && !recipient.getFcmToken().isEmpty()) {
                sendDMNotificationToUser(recipient, chatId, senderUsername, messageContent, chatRoom);
            }
        } catch (Exception e) {
            log.error("❌ Error sending DM notification: {}", e.getMessage());
        }
    }

    private void sendGroupNotification(String chatId, String senderUsername,
                                       String messageContent, String chatName, ChatRoom chatRoom) {
        try {
            List<User> recipients = userRepo.findByChatRoomsChatIdAndUsernameNot(chatId, senderUsername);

            recipients = recipients.stream()
                    .filter(user -> !user.getUsername().equals(senderUsername))
                    .collect(Collectors.toList());

            int successfulSends = 0;
            int failedSends = 0;

            for (User recipient : recipients) {
                if (recipient.getFcmToken() != null && !recipient.getFcmToken().isEmpty()) {
                    boolean success = sendGroupNotificationToUser(recipient, chatId, senderUsername, messageContent, chatName, chatRoom);
                    if (success) {
                        successfulSends++;
                    } else {
                        failedSends++;
                    }
                }
            }
            log.info("📤 Group notification summary - Chat: {}, Successful: {}, Failed: {}",
                    chatName, successfulSends, failedSends);

        } catch (Exception e) {
            log.error("❌ Error sending group notification: {}", e.getMessage());
        }
    }

    private void sendDMNotificationToUser(User recipient, String chatId, String senderUsername,
                                          String messageContent, ChatRoom chatRoom) {
        try {

            String displayName = chatRoom.getDisplayName(recipient.getUsername());
            String pfpIndex = chatRoom.getChatPfpIndex(recipient.getUsername());
            String pfpBg = chatRoom.getChatPfpBg(recipient.getUsername());

            String groupKey = "chat_" + chatId;

//            AndroidNotification androidNotification = AndroidNotification.builder()
//                    .setIcon("ic_notification")
//                    .setColor("#4CAF50")
//                    .setSound("default")
//                    .setTag(groupKey)
//                    .build();

            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
//                    .setNotification(androidNotification)
                    .setCollapseKey(groupKey)
                    .build();

            Message message = Message.builder()
                    .setToken(recipient.getFcmToken())
//                    .setNotification(Notification.builder()
//                            .setTitle(senderUsername)
//                            .setBody(messageContent)
//                            .build())
                    .setAndroidConfig(androidConfig)
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setBadge(1)
                                    .setSound("default")
                                    .build())
                            .build())
                    .setWebpushConfig(WebpushConfig.builder()
                            .setNotification(WebpushNotification.builder()
                                    .setIcon("/icons/icon-192x192.png")
                                    .setBadge("/icons/badge-72x72.png")
                                    .build())
                            .build())
                    .putData("chatId", chatId)
                    .putData("sender", senderUsername)
                    .putData("message", messageContent)
                    .putData("chatName", displayName)
                    .putData("pfpIndex", pfpIndex)
                    .putData("pfpBg", pfpBg)
                    .putData("isGroup", "false")
                    .putData("type", "NEW_MESSAGE")
                    .build();

            String response = firebaseMessaging.send(message);
            log.info("📤 DM notification sent to {} from {}: {}", recipient.getUsername(), senderUsername, response);
        } catch (FirebaseMessagingException e) {
            log.error("❌ Failed to send DM notification to {}: {}", recipient.getUsername(), e.getMessage());
            handleInvalidToken(recipient, e);
        }
    }

    private boolean sendGroupNotificationToUser(User recipient, String chatId, String senderUsername,
                                                String messageContent, String chatName, ChatRoom chatRoom) {
        try {

            String pfpIndex = chatRoom.getChatPfpIndex(recipient.getUsername());
            String pfpBg = chatRoom.getChatPfpBg(recipient.getUsername());

            String groupKey = "chat_" + chatId;

//            AndroidNotification androidNotification = AndroidNotification.builder()
//                    .setIcon("ic_notification")
//                    .setColor("#4CAF50")
//                    .setSound("default")
//                    .setTag(groupKey)
//                    .build();

            AndroidConfig androidConfig = AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
//                    .setNotification(androidNotification)
                    .setCollapseKey(groupKey)
                    .build();

            Message message = Message.builder()
                    .setToken(recipient.getFcmToken())
//                    .setNotification(Notification.builder()
//                            .setTitle(chatName)
//                            .setBody(senderUsername + ": " + messageContent)
//                            .build())
                    .setAndroidConfig(androidConfig)
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setBadge(1)
                                    .setSound("default")
                                    .build())
                            .build())
                    .setWebpushConfig(WebpushConfig.builder()
                            .setNotification(WebpushNotification.builder()
                                    .setIcon("/icons/icon-192x192.png")
                                    .setBadge("/icons/badge-72x72.png")
                                    .build())
                            .build())
                    .putData("chatId", chatId)
                    .putData("sender", senderUsername)
                    .putData("message", messageContent)
                    .putData("chatName", chatName)
                    .putData("pfpIndex", pfpIndex)
                    .putData("pfpBg", pfpBg)
                    .putData("isGroup", "true")
                    .putData("type", "NEW_MESSAGE")
                    .build();

            String response = firebaseMessaging.send(message);
            log.debug("✅ Group notification sent to {} for group {}: {}",
                    recipient.getUsername(), chatName, response);
            return true;
        } catch (FirebaseMessagingException e) {
            log.error("❌ Failed to send group notification to {}: {}", recipient.getUsername(), e.getMessage());
            handleInvalidToken(recipient, e);
            return false;
        }
    }

    private void handleInvalidToken(User user, FirebaseMessagingException e) {
        if (e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT ||
                e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
            user.setFcmToken(null);
            userRepo.save(user);
            log.info("🗑️ Removed invalid FCM token for user: {}", user.getUsername());
        }
    }

    public void sendGroupInvitationNotification(String groupName, List<String> usernames) {
        try {
            List<User> users = userRepo.findAllByUsernames(usernames);

            int successfulSends = 0;
            int failedSends = 0;

            for (User user : users) {
                if (user.getFcmToken() != null && !user.getFcmToken().trim().isEmpty()) {
                    boolean success = sendSingleGroupInvitation(user, groupName);
                    if (success) {
                        successfulSends++;
                    } else {
                        failedSends++;
                    }
                } else {
                    log.info("ℹ️ User {} has no FCM token, skipping notification", user.getUsername());
                    failedSends++;
                }
            }

            log.info("📤 Group invitation notification summary - Group: {}, Successful: {}, Failed: {}",
                    groupName, successfulSends, failedSends);

        } catch (Exception e) {
            log.error("❌ Error sending group invitation notifications for group {}: {}", groupName, e.getMessage());
        }
    }

    private boolean sendSingleGroupInvitation(User user, String groupName) {
        try {
            Message message = Message.builder()
                    .setToken(user.getFcmToken())
                    .setNotification(Notification.builder()
                            .setTitle("Group Invitation")
                            .setBody("You've been invited to join " + groupName)
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setIcon("ic_notification")
                                    .setColor("#FF9800")
                                    .setSound("default")
                                    .build())
                            .build())
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setBadge(1)
                                    .setSound("default")
                                    .build())
                            .build())
                    .setWebpushConfig(WebpushConfig.builder()
                            .setNotification(WebpushNotification.builder()
                                    .setIcon("/icons/icon-192x192.png")
                                    .setBadge("/icons/badge-72x72.png")
                                    .build())
                            .build())
                    .putData("type", "GROUP_INVITATION")
                    .putData("groupName", groupName)
                    .putData("timestamp", String.valueOf(System.currentTimeMillis()))
                    .build();

            String response = firebaseMessaging.send(message);
            log.debug("✅ Group invitation sent to {} for group: {}", user.getUsername(), groupName);
            return true;

        } catch (FirebaseMessagingException e) {
            log.error("❌ Failed to send group invitation to {}: {}", user.getUsername(), e.getMessage());
            handleInvalidToken(user, e);
            return false;
        } catch (Exception e) {
            log.error("❌ Unexpected error sending group invitation to {}: {}", user.getUsername(), e.getMessage());
            return false;
        }
    }
}