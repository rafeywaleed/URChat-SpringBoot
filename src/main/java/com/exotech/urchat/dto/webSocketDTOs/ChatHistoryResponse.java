package com.exotech.urchat.dto.webSocketDTOs;

import lombok.Data;

@Data
public class ChatHistoryResponse {
    private String chatId;
    private Object messages;
    private boolean success;
    private String error;

    public ChatHistoryResponse(String chatId, Object messages, boolean success) {
        this.chatId = chatId;
        this.messages = messages;
        this.success = success;
    }

    public ChatHistoryResponse(String chatId, Object messages, boolean success, String error) {
        this.chatId = chatId;
        this.messages = messages;
        this.success = success;
        this.error = error;
    }
}
