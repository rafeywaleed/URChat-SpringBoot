package com.exotech.urchat.controller;

import com.exotech.urchat.dto.messageDTOs.MessageDTOConvertor;
import com.exotech.urchat.dto.chatDTOs.ChatRoomDTO;
import com.exotech.urchat.dto.webSocketDTOs.ChatHistoryResponse;
import com.exotech.urchat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final MessageDTOConvertor messageDTOConvertor;

    @SubscribeMapping("/user/queue/chat/{chatId}")
    public ChatHistoryResponse getInitialChatData(
            @DestinationVariable String chatId,
            Principal principal) {

        String username = principal.getName();
        log.info("User {} requesting initial data for chat {}", username, chatId);
        try {
//            List<MessageDTO> messages = chatService.getChatMessages(chatId, username);
            var messages = chatService.getPaginatedMessages(chatId, 0, 20, username);
            return new ChatHistoryResponse(chatId, messages, true);
        } catch (Exception e) {
            log.error("Error getting chat history for user {}: {}", username, e.getMessage());
            return new ChatHistoryResponse(chatId, null, false, e.getMessage());
        }
    }

    @SubscribeMapping("/user/queue/chats")
    public List<ChatRoomDTO> getInitalChatList(Principal principal){
        String username = principal.getName();
        log.info("User {} subscribing to chat list updates", username);
        return chatService.getUserChats(username);
    }

    private void updateChatListForUser(String username) {
        try {
            List<ChatRoomDTO> updatedChats = chatService.getUserChats(username);
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/chats/update",
                    updatedChats
            );
            log.debug("Sent chat list update to user: {}", username);
        } catch (Exception e) {
            log.error("Error updating chat list for user {}: {}", username, e.getMessage());
        }
    }

}
