package com.exotech.urchat.controller;

import com.exotech.urchat.dto.messageDTOs.MessageDTO;
import com.exotech.urchat.dto.chatDTOs.*;
import com.exotech.urchat.dto.messageDTOs.MessageStatsDTO;
import com.exotech.urchat.repository.MessageRepo;
import com.exotech.urchat.service.ChatService;
import com.exotech.urchat.service.GroupChatService;
import com.exotech.urchat.service.MessageCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final MessageCleanupService messageCleanupService;
    private final ChatService chatService;
    private final GroupChatService groupChatService;
    private final MessageRepo messageRepo;

    @GetMapping("/chats")
    public ResponseEntity<List<ChatRoomDTO>> getUserChats(@AuthenticationPrincipal String username) {

        System.out.println("Entered Get Users Chats Controller");
        List<ChatRoomDTO> chats = chatService.getUserChats(username);
        return ResponseEntity.ok(chats);
    }

    @PostMapping("/individual")
    public ResponseEntity<ChatRoomDTO> createIndividualChat(
            @AuthenticationPrincipal String username,
            @RequestParam String withUser) {
        ChatRoomDTO chat = chatService.getOrCreateIndividualChat(username, withUser);
        return ResponseEntity.ok(chat);
    }

    @PostMapping("/group")
    public ResponseEntity<GroupChatRoomDTO> createGroup(
            @AuthenticationPrincipal String username,
            @RequestBody CreateGroupRequest request) {
        GroupChatRoomDTO group = groupChatService.createGroup(request.getName(), username, request.getParticipants());
        return ResponseEntity.ok(group);
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<MessageDTO>> getChatMessages(
            @AuthenticationPrincipal String username,
            @PathVariable String chatId) {
        List<MessageDTO> messages = chatService.getChatMessages(chatId, username);
//        List<MessageDTO> messages = chatService.getPaginatedMessages(chatId, 0, 50, username);
        System.out.println("✅ Loaded " + messages.size() + " messages for chat: " + chatId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/{chatId}/messages/paginated")
    public ResponseEntity<List<MessageDTO>> getPaginatedMessages(
            @AuthenticationPrincipal String username,
            @PathVariable String chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<MessageDTO> messages = chatService.getPaginatedMessages(chatId, page, size, username);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/group/invitations")
    public ResponseEntity<List<ChatRoomDTO>> getGroupInvitations(@AuthenticationPrincipal String username) {
        List<ChatRoomDTO> invitations = groupChatService.groupChatInvitations(username);
        return ResponseEntity.ok(invitations);
    }

    @PostMapping("/group/{chatId}/accept")
    public ResponseEntity<String> acceptGroupInvitation(
            @AuthenticationPrincipal String username,
            @PathVariable String chatId) {
        groupChatService.acceptGroupInvitation(chatId, username);
        return ResponseEntity.ok("Group invitation accepted");
    }

    @PostMapping("/group/{chatId}/decline")
    public ResponseEntity<String> declineGroupInvitation(
            @AuthenticationPrincipal String username,
            @PathVariable String chatId) {
        groupChatService.declineGroupInvitation(chatId, username);
        return ResponseEntity.ok("Group invitation declined");
    }

    @PostMapping("/group/{chatId}/leave")
    public ResponseEntity<String> leaveGroup(
            @AuthenticationPrincipal String username,
            @PathVariable String chatId) {
        groupChatService.leaveGroup(chatId, username);
        return ResponseEntity.ok("Left group successfully");
    }

    @PostMapping("/group/{chatId}/invite")
    public ResponseEntity<String> inviteToGroup(
            @AuthenticationPrincipal String username,
            @PathVariable String chatId,
            @RequestParam String inviteeUsername) {
        groupChatService.inviteToGroup(chatId, username, inviteeUsername);
        return ResponseEntity.ok("User invited to group");
    }

    @PostMapping("/group/{chatId}/remove")
    public ResponseEntity<String> removeFromGroup(
            @AuthenticationPrincipal String username,
            @PathVariable String chatId,
            @RequestParam String removeUsername) {
        groupChatService.removeFromGroup(chatId, username, removeUsername);
        return ResponseEntity.ok("User removed from group");
    }

    @DeleteMapping("/group/{chatId}/delete")
    public void deleteGroup(@PathVariable String chatId){
//        groupChatService.deleteGroup(chatId);
        groupChatService.deleteAllGroups();

    }

    @GetMapping("/group/{chatId}/details")
    public ResponseEntity<GroupChatRoomDTO> getGroupDetails(
            @AuthenticationPrincipal String username,
            @PathVariable String chatId) {
        GroupChatRoomDTO groupDetails = groupChatService.getGroupDetails(chatId, username);
        return ResponseEntity.ok(groupDetails);
    }

    @GetMapping("/groups/search")
    public ResponseEntity<List<ChatRoomDTO>> searchGroups(@RequestParam String name) {
        List<ChatRoomDTO> groups = groupChatService.searchGroups(name);
        return ResponseEntity.ok(groups);
    }

    @PutMapping("/group/{chatId}/updatePfp")
    public ResponseEntity<PfpDTO> updateGroupPfp(
            @AuthenticationPrincipal String username,
            @PathVariable String chatId,
            @RequestBody PfpDTO pfpDTO
            ) {
        PfpDTO pfp = groupChatService.updateGroupPfp(chatId, pfpDTO);
        return ResponseEntity.ok(pfp);
    }

    @GetMapping("/theme/{chatId}")
    public ResponseEntity<ChatThemeDTO> getTheme(
            @AuthenticationPrincipal String username,
            @PathVariable String chatId){
        ChatThemeDTO theme = chatService.getTheme(chatId);
        return ResponseEntity.ok(theme);
    }

    @PutMapping("/theme/{chatId}/change")
    public ResponseEntity<ChatThemeDTO> setTheme(
            @AuthenticationPrincipal String username,
            @PathVariable String chatId,
            @RequestBody ChatThemeDTO newTheme){
        System.out.println("Set Theme Controller");
        ChatThemeDTO theme = chatService.setTheme(chatId, newTheme);
        return ResponseEntity.ok(theme);
    }

    @DeleteMapping("/message/{messageId}")
    public ResponseEntity<String> deleteMessage(
            @AuthenticationPrincipal String username,
            @PathVariable Long messageId) {
        chatService.deleteMessage(messageId, username);
        return ResponseEntity.ok("Message deleted successfully");
    }

    @DeleteMapping("/{chatId}")
    public ResponseEntity<String> deleteChat(
            @AuthenticationPrincipal String username,
            @PathVariable String chatId) {
        chatService.deleteChat(chatId, username);
        return ResponseEntity.ok("Chat deleted successfully");
    }

    @PostMapping("/{chatId}/leave")
    public ResponseEntity<String> leaveChat(
            @AuthenticationPrincipal String username,
            @PathVariable String chatId) {
        chatService.leaveGroup(chatId, username);
        return ResponseEntity.ok("Left chat successfully");
    }

    @PostMapping("/admin/cleanup-messages")
    public ResponseEntity<String> triggerManualCleanup(@AuthenticationPrincipal String username) {
        // Only allow admin users to trigger manual cleanup
        if (!isAdmin(username)) {
            return ResponseEntity.status(403).body("Only admins can trigger manual cleanup");
        }

        try {
            messageCleanupService.deleteMessagesOlderThan30Days();
            return ResponseEntity.ok("Manual cleanup completed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Cleanup failed: " + e.getMessage());
        }
    }

    @GetMapping("/admin/message-stats")
    public ResponseEntity<MessageStatsDTO> getMessageStatistics(@AuthenticationPrincipal String username) {
        if (!isAdmin(username)) {
            return ResponseEntity.status(403).build();
        }

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long totalMessages = messageRepo.count();
        long oldMessages = messageRepo.countMessagesOlderThan(thirtyDaysAgo);
        long recentMessages = totalMessages - oldMessages;

        MessageStatsDTO stats = new MessageStatsDTO(totalMessages, recentMessages, oldMessages);
        return ResponseEntity.ok(stats);
    }

    private boolean isAdmin(String username) {
        // Implement your admin check logic here
        // This could check against a list of admin usernames or a role in the user entity
        return Arrays.asList("admin", "administrator").contains(username.toLowerCase());
    }
}