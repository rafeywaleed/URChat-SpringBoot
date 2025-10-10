package com.exotech.urchat.dto.messageDTOs;

import com.exotech.urchat.model.Message;
import org.springframework.stereotype.Component;

@Component
public class MessageDTOConvertor {

    public MessageDTO convertToMessageDTO(Message message, String currentUsername) {
        return MessageDTO.builder()
                .id(message.getMessageId())
                .content(message.getMessageContent())
                .sender(message.getSender().getUsername())
                .chatId(message.getChatRoom().getChatId())
                .timestamp(message.getTimestamp())
                .isOwnMessage(message.getSender().getUsername().equals(currentUsername))
                .build();
    }

}
