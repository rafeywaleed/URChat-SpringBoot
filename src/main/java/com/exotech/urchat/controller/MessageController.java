package com.exotech.urchat.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MessageController {

//    private final ChatService chatService;
//
//    @PostMapping("/send")
//    public ResponseEntity<MessageDTO> sendMessage(
//            @RequestBody SendMessageRequest request,
//            Principal principal) {
//        try {
//            String senderUsername = principal.getName();
//            var message = chatService.sendMessage(senderUsername, request.getChatId(), request.getContent());
//
//            MessageDTO dto = MessageDTO.builder()
//                    .messageId(message.getMessageId())
//                    .messageContent(message.getMessageContent())
//                    .senderUsername(message.getSenderUsername())
//                    .chatId(message.getChatId())
//                    .timestamp(message.getTimestamp())
//                    .build();
//
//            return ResponseEntity.ok(dto);
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(null);
//        }
//    }
//
//    @GetMapping("/{chatId}")
//    public ResponseEntity<List<MessageDTO>> getChatMessages(
//            @PathVariable String chatId,
//            Principal principal) {
//        try {
//            String username = principal.getName();
//            List<MessageDTO> messages = chatService.getChatMessages(chatId, username);
//            return ResponseEntity.ok(messages);
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(null);
//        }
//    }
//
//    @GetMapping("/{chatId}/paginated")
//    public ResponseEntity<List<MessageDTO>> getPaginatedMessages(
//            @PathVariable String chatId,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "50") int size,
//            Principal principal) {
//        try {
//            String currentUser = principal.getName();
//            List<MessageDTO> messages = chatService.getPaginatedMessages(chatId, page, size, currentUser);
//            return ResponseEntity.ok(messages);
//        } catch (RuntimeException e) {
//            return ResponseEntity.badRequest().body(null);
//        }
//    }

//    // Request DTO for sending messages
//    public static class SendMessageRequest {
//        private String chatId;
//        private String content;
//
//        // Getters and setters
//        public String getChatId() { return chatId; }
//        public void setChatId(String chatId) { this.chatId = chatId; }
//        public String getContent() { return content; }
//        public void setContent(String content) { this.content = content; }
//    }
}