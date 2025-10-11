package com.exotech.urchat.controller;

import com.exotech.urchat.dto.messageDTOs.DeleteMessageRequest;
import com.exotech.urchat.dto.messageDTOs.MessageDTO;
import com.exotech.urchat.dto.messageDTOs.MessageDTOConvertor;
import com.exotech.urchat.dto.chatDTOs.ChatRoomDTO;
import com.exotech.urchat.dto.webSocketDTOs.*;
import com.exotech.urchat.model.ChatRoom;
import com.exotech.urchat.model.Message;
import com.exotech.urchat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final MessageDTOConvertor messageDTOConvertor;


//    private final Map<String, String> userCurrentChats = new ConcurrentHashMap<>();
//    private final ConcurrentHashMap<String, Boolean> processedMessages = new ConcurrentHashMap<>();



    private void updateChatListForUser(String username) {
        try {
            List<ChatRoomDTO> updatedChats = chatService.getUserChats(username);

            log.info("🚀 SENDING CHAT LIST UPDATE TO USER: {}", username);
            log.info("   📤 Destination: /user/{}/queue/chats/update", username);
            log.info("   📦 Number of chats: {}", updatedChats.size());

            for (ChatRoomDTO chat : updatedChats) {
                log.info("   💬 Chat: {} | LastMsg: {} | Time: {}",
                        chat.getChatName(), chat.getLastMessage(), chat.getLastActivity());
            }

            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/chats/update",
                    updatedChats
            );

            log.info("✅ Successfully sent chat list update to user: {}", username);

        } catch (Exception e) {
            log.error("❌ Error updating chat list for user {}: {}", username, e.getMessage());
        }
    }

    @MessageMapping("/chat/{chatId}/send")
    public void sendMessage(
            @DestinationVariable String chatId,
            @Payload ChatMessageRequest messageRequest,
            Principal principal) {

        System.out.println("Send message is called");

        String username = principal.getName();
        log.info("User {} sending message to chat {}: {}", username, chatId, messageRequest.getContent());

        try {
            Message savedMessage = chatService.sendMessage(username, chatId, messageRequest.getContent());
            MessageDTO messageDTO = messageDTOConvertor.convertToMessageDTO(savedMessage, username);

            messagingTemplate.convertAndSend("/topic/chat/" + chatId, messageDTO);

            chatService.updateChatListsForParticipants(chatId, username);

//            updateChatListsForParticipants(chatId, username);
//            broadcastChatListUpdate(chatId, username);

//            if (processedMessages.size() > 1000) {
//                processedMessages.clear();
//            }

            log.info("Message broadcasted to chat {}: {}", chatId, savedMessage.getMessageId());

        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage());
//            processedMessages.remove(messageKey);
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/errors",
                    new ErrorMessage("MESSAGE_SEND_FAILED", e.getMessage())
            );
        }
    }

    @MessageMapping("/chat/{chatId}/typing")
    public void handleTyping(
            @DestinationVariable String chatId,
            @Payload TypingNotification typing,
            Principal principal) {

        String username = principal.getName();
//        if (typing.isTyping()) {
//            userCurrentChats.put(username, chatId);
//        } else {
//            userCurrentChats.remove(username);
//        }
        TypingBroadcast broadcast = new TypingBroadcast(username, typing.isTyping());
        messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/typing", broadcast);

        log.debug("User {} {} in chat {}", username, typing.isTyping() ? "started typing" : "stopped typing", chatId);
    }

    @MessageMapping("/chat/{chatId}/read")
    public void handleReadReceipt(
            @DestinationVariable String chatId,
            @Payload ReadReceipt receipt,
            Principal principal) {

//        ghm pvt limited
        String username = principal.getName();
        ReadReceiptBroadcast broadcast = new ReadReceiptBroadcast(
                username, receipt.getMessageId(), LocalDateTime.now()
        );
        messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/read", broadcast);
        log.debug("User {} read message {} in chat {}", username, receipt.getMessageId(), chatId);
    }

    @MessageMapping("/chat/create-individual")
    public void createIndividualChat(
            @Payload CreateIndividualChatRequest request,
            Principal principal
    ) {
        String username = principal.getName();
        String targetUsername = request.getTargetUsername();

        try {
            ChatRoomDTO chat = chatService.getOrCreateIndividualChat(username, targetUsername);

            updateChatListForUser(username);
            updateChatListForUser(targetUsername);

        } catch (Exception e) {
            log.error("Error creating individual chat: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/errors",
                    new ErrorMessage("CHAT_CREATION_FAILED", e.getMessage())
            );
        }
    }

    private void broadcastChatListUpdate(String chatId, String excludingUsername){
        try{
            ChatRoom chatRoom = chatService.getChatRoom(chatId);

            chatRoom.getParticipants().forEach(participant ->{
//                if(!participant.getUsername().equals(excludingUsername)){
                    updateChatListForUser(participant.getUsername());
//                }
            });
            updateChatListForUser(excludingUsername);
        } catch (Exception e){
            log.error("Error broadcasting chat list update: {}", e.getMessage());
        }
    }

//    public boolean isUserTypingInChat(String username, String chatId) {
//        return chatId.equals(userCurrentChats.get(username));
//    }

//    @Scheduled(fixedRate = 300000)
//    public void cleanupProcessedMessages() {
//        processedMessages.clear();
//    }




    @Transactional
    public void  updateChatListsForParticipants(String chatId,String username){
        try {
            ChatRoom chatRoom = chatService.getChatRoom(chatId);

            chatRoom.getParticipants().forEach(participant -> {
                if (!participant.getUsername().equals(username)) {
                    updateChatListForUser(participant.getUsername());
                }
            });

            updateChatListForUser(username);

        } catch (Exception e) {
            log.error("Error updating chat lists: {}", e.getMessage());
        }
    }


    @MessageMapping("/chat/{chatId}/delete-message")
    public void deleteMessage(
            @DestinationVariable String chatId,
            @Payload DeleteMessageRequest request,
            Principal principal) {

        String username = principal.getName();
        log.info("User {} deleting message {} from chat {}", username, request.getMessageId(), chatId);

        try {
            chatService.deleteMessage(request.getMessageId(), username);

            // The broadcast is now handled in the service layer
            log.info("Message deletion completed for message {} in chat {}", request.getMessageId(), chatId);

        } catch (Exception e) {
            log.error("Error deleting message: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/errors",
                    new ErrorMessage("MESSAGE_DELETE_FAILED", e.getMessage())
            );
        }
    }

    // Add this method for handling chat deletion via WebSocket
    @MessageMapping("/chat/{chatId}/delete-chat")
    public void deleteChat(
            @DestinationVariable String chatId,
            Principal principal) {

        String username = principal.getName();
        log.info("User {} deleting chat {}", username, chatId);

        try {
            chatService.deleteChat(chatId, username);

            // The broadcast is handled in the service layer
            log.info("Chat deletion completed for chat {} by user {}", chatId, username);

        } catch (Exception e) {
            log.error("Error deleting chat: {}", e.getMessage());
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/errors",
                    new ErrorMessage("CHAT_DELETE_FAILED", e.getMessage())
            );
        }
    }

}