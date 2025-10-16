package com.exotech.urchat.service;

import com.exotech.urchat.dto.messageDTOs.MessageDTOConvertor;
import com.exotech.urchat.dto.chatDTOs.*;
import com.exotech.urchat.model.ChatRoom;
import com.exotech.urchat.model.User;
import com.exotech.urchat.repository.ChatRoomRepo;
import com.exotech.urchat.repository.MessageRepo;
import com.exotech.urchat.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupChatService {

    private final ChatRoomRepo chatRoomRepo;
    private final MessageRepo messageRepo;
    private final UserRepo userRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageDTOConvertor messageDTOConvertor;
    private final ChatDTOConvertor chatDTOConvertor;
    private final ChatService chatService;
    private final NotificationService notificationService;

    @Transactional
    public GroupChatRoomDTO createGroup(String name, String adminUsername, List<String> participantUsernames) {
        if (participantUsernames == null || participantUsernames.isEmpty()) {
            throw new RuntimeException("Group must have at least one participant");
        }

        if (participantUsernames.size() == 1) {
//            String otherUser = participantUsernames.get(0);
//            return chatService.getOrCreateIndividualChat(adminUsername, otherUser);
            throw new RuntimeException("Cannot create group with 2 members");
        }

        User admin = userRepo.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("User " + adminUsername + " not found"));

        ChatRoom group = new ChatRoom();
        group.setChatId(UUID.randomUUID().toString());
        group.setIsGroup(true);
        group.setChatName(name);
        group.setAdmin(admin);
        group.setPfpIndex("👥");
        group.setPfpBg("#FF9800");
        group.setLastActivity(LocalDateTime.now());

        group.getParticipants().add(admin);
        List<GroupMembersDTO> membersDTO = new ArrayList<>();
        membersDTO.add(chatDTOConvertor.convertToGroupMembersDTO(admin,true, true));

        List<GroupMembersDTO> pendingMembersDTO = new ArrayList<>();

        List<String> invitedUsernames = new ArrayList<>();

        for (String username : participantUsernames) {
            if (!username.equals(adminUsername)) {
                User user = userRepo.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User " + username + " not found"));
                group.getPendingInvitations().add(user);
                pendingMembersDTO.add(chatDTOConvertor.convertToGroupMembersDTO(user, false, false));

                invitedUsernames.add(username);
            }
        }
        ChatRoom chat =  chatRoomRepo.save(group);

        if (!invitedUsernames.isEmpty()) {
            try {
                notificationService.sendGroupInvitationNotification(name, invitedUsernames);
                log.info("📤 Sent group invitation notifications for group: {}", name);
            } catch (Exception e) {
                log.error("❌ Failed to send group invitation notifications: {}", e.getMessage());
                // Don't throw exception - group creation should still succeed
            }
        }


        return chatDTOConvertor.convertToGroupChatRoomDTO(chat, adminUsername, membersDTO, pendingMembersDTO);
    }

    public List<ChatRoomDTO> groupChatInvitations(String username) {
        List<ChatRoom> invitations = chatRoomRepo.findGroupInvitations(username);
        return invitations.stream()
                .map(chatDTOConvertor::convertToChatRoomDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void acceptGroupInvitation(String chatId, String username) {
        ChatRoom chat = chatRoomRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        chat.getPendingInvitations().removeIf(invitee -> invitee.getUsername().equals(username));
        if (!chat.hasParticipant(username)) {
            chat.getParticipants().add(user);
        }
        chatRoomRepo.save(chat);
    }

    @Transactional
    public void declineGroupInvitation(String chatId, String username) {
        ChatRoom chat = chatRoomRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        chat.getPendingInvitations().removeIf(invitee -> invitee.getUsername().equals(username));
        chatRoomRepo.save(chat);
    }

    @Transactional
    public void leaveGroup(String chatId, String username) {
        ChatRoom chat = chatRoomRepo.findByIdWithParticipants(chatId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!chat.hasParticipant(username)) {
            throw new RuntimeException("You are not a member of this group");
        }

        // Store participants before removal to check if group becomes empty
        int participantsBeforeRemoval = chat.getParticipants().size();

        // Remove user from participants
        chat.getParticipants().removeIf(participant -> participant.getUsername().equals(username));

        // Also remove from pending invitations if present
        chat.getPendingInvitations().removeIf(invitee -> invitee.getUsername().equals(username));

        // If admin leaves and there are other participants, assign new admin
        if (chat.getAdmin().getUsername().equals(username)) {
            if (!chat.getParticipants().isEmpty()) {
                User newAdmin = chat.getParticipants().get(0); // Assign first participant as new admin
                chat.setAdmin(newAdmin);
                log.info("🚨 Admin {} left group {}. Transferred admin rights to {}",
                        username, chatId, newAdmin.getUsername());
            } else {
                // If no participants left after admin leaves, DELETE THE ENTIRE GROUP
                log.info("🚨 Admin {} left group {} and no participants remain. DELETING GROUP", username, chatId);
                permanentlyDeleteGroup(chatId);
                return;
            }
        }

        // Check if group becomes empty after this user leaves
        if (participantsBeforeRemoval == 1 && chat.getParticipants().isEmpty()) {
            // This was the last participant, DELETE THE ENTIRE GROUP
            log.info("🚨 Last participant {} left group {}. DELETING EMPTY GROUP", username, chatId);
            permanentlyDeleteGroup(chatId);
            return;
        }

        // Save changes if group still has participants
        chatRoomRepo.save(chat);
        log.info("✅ User {} left group {}. Remaining participants: {}",
                username, chatId, chat.getParticipants().size());

        // Update chat lists for all remaining participants
        updateChatListsForAllParticipants(chatId);
    }

    @Transactional
    public void permanentlyDeleteGroup(String chatId) {
        try {
            ChatRoom chat = chatRoomRepo.findById(chatId)
                    .orElseThrow(() -> new RuntimeException("Group not found"));

            log.info("🗑️ Starting permanent deletion of group {} and all its messages", chatId);

            // 1. First delete all messages from the group
            int deletedMessages = messageRepo.deleteAllByChatId(chatId);
            log.info("✅ Deleted {} messages from group {}", deletedMessages, chatId);

            // 2. Clear all participants and pending invitations (clean up relationships)
            chat.getParticipants().clear();
            chat.getPendingInvitations().clear();

            // 3. Delete the group itself from database
            chatRoomRepo.delete(chat);
            log.info("✅ PERMANENTLY deleted group {} from database", chatId);

            // 4. Broadcast group deletion to all users who might have it in their lists
            broadcastGroupDeletion(chatId);

            // 5. Verify deletion
            verifyGroupDeletion(chatId);

        } catch (Exception e) {
            log.error("❌ Error permanently deleting group {}: {}", chatId, e.getMessage());
            throw new RuntimeException("Failed to delete group: " + e.getMessage(), e);
        }
    }

    private void verifyGroupDeletion(String chatId) {
        boolean groupExists = chatRoomRepo.existsById(chatId);
        long remainingMessages = messageRepo.countByChatRoomChatId(chatId);

        if (!groupExists && remainingMessages == 0) {
            log.info("✅ VERIFIED: Group {} and all messages successfully deleted from database", chatId);
        } else {
            log.error("❌ VERIFICATION FAILED: Group {} still exists: {}, remaining messages: {}",
                    chatId, groupExists, remainingMessages);
            throw new RuntimeException("Group deletion verification failed");
        }
    }

    private void broadcastGroupDeletion(String chatId) {
        try {
            messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/chat-deleted", chatId);
            log.info("Broadcasted group deletion {} to all users", chatId);
        } catch (Exception e) {
            log.error("Error broadcasting group deletion: {}", e.getMessage());
        }
    }

    private void updateChatListsForAllParticipants(String chatId) {
        try {
            ChatRoom chat = chatRoomRepo.findByIdWithParticipants(chatId)
                    .orElseThrow(() -> new RuntimeException("Chat not found"));

            for (User participant : chat.getParticipants()) {
                updateChatListForUser(participant.getUsername());
            }
        } catch (Exception e) {
            log.error("Error updating chat lists for participants: {}", e.getMessage());
        }
    }

    private void updateChatListForUser(String username) {
        try {
            List<ChatRoomDTO> updatedChats = getUserChats(username);
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/chats/update",
                    updatedChats
            );
        } catch (Exception e) {
            log.error("Error updating chat list for user {}: {}", username, e.getMessage());
        }
    }

    // Add this method to get user chats
    private List<ChatRoomDTO> getUserChats(String username) {
        List<ChatRoom> chats = chatRoomRepo.findByParticipantUsername(username);
        chats.sort((a, b) -> b.getLastActivity().compareTo(a.getLastActivity()));

        return chats.stream()
                .map(chat -> {
                    ChatRoomDTO dto = chatDTOConvertor.convertToChatRoomDTO(chat);
                    dto.setChatName(chat.getDisplayName(username));
                    dto.setPfpIndex(chat.getChatPfpIndex(username));
                    dto.setPfpBg(chat.getChatPfpBg(username));
                    dto.setLastMessage(chat.getLastMessage());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void inviteToGroup(String chatId, String inviterUsername, String inviteeUsername) {
        ChatRoom chat = chatRoomRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User inviter = userRepo.findByUsername(inviterUsername)
                .orElseThrow(() -> new RuntimeException("Inviter not found"));
        User invitee = userRepo.findByUsername(inviteeUsername)
                .orElseThrow(() -> new RuntimeException("Invitee not found"));

        if (!chat.getAdmin().getUsername().equals(inviterUsername)) {
            throw new RuntimeException("Only admin can invite users");
        }

        if (chat.hasParticipant(inviteeUsername)) {
            throw new RuntimeException("User is already a member");
        }
        if (chat.hasPendingInvitation(inviteeUsername)) {
            throw new RuntimeException("User already has a pending invitation");
        }

        chat.getPendingInvitations().add(invitee);
        chatRoomRepo.save(chat);
    }

    @Transactional
    public void removeFromGroup(String chatId, String removerUsername, String toRemoveUsername) {
        ChatRoom chat = chatRoomRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User remover = userRepo.findByUsername(removerUsername)
                .orElseThrow(() -> new RuntimeException("Remover not found"));
        User toRemove = userRepo.findByUsername(toRemoveUsername)
                .orElseThrow(() -> new RuntimeException("User to remove not found"));

        if (!chat.getAdmin().getUsername().equals(removerUsername)) {
            throw new RuntimeException("Only admin can remove users");
        }

        if (toRemoveUsername.equals(removerUsername)) {
            throw new RuntimeException("Admin cannot remove themselves. Use leave group instead.");
        }

        chat.getParticipants().removeIf(participant -> participant.getUsername().equals(toRemoveUsername));
        chat.getPendingInvitations().removeIf(invitee -> invitee.getUsername().equals(toRemoveUsername));
        chatRoomRepo.save(chat);
    }

    public GroupChatRoomDTO getGroupDetails(String chatId, String currentUser) {
        ChatRoom chat = chatRoomRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        boolean isParticipant = chat.hasParticipant(currentUser);
        if (!isParticipant) {
            throw new RuntimeException("Access denied to this group");
        }

        List<GroupMembersDTO> memberDTOs = chat.getParticipants().stream()
                .map(user -> chatDTOConvertor.convertToGroupMembersDTO(
                        user,
                        chat.getAdmin().getUsername().equals(user.getUsername()),
                        true))
                .collect(Collectors.toList());

        List<GroupMembersDTO> requestDTOs = chat.getPendingInvitations().stream()
                .map(user -> chatDTOConvertor.convertToGroupMembersDTO(user, false, false))
                .collect(Collectors.toList());

        return chatDTOConvertor.convertToGroupChatRoomDTO(
                chat, chat.getAdmin().getUsername(), memberDTOs, requestDTOs);
    }

    public List<ChatRoomDTO> searchGroups(String searchName) {
        List<ChatRoom> groups = chatRoomRepo.findGroupsByNameContaining(searchName);
        return groups.stream()
                .map(chatDTOConvertor::convertToChatRoomDTO)
                .collect(Collectors.toList());
    }

    public PfpDTO updateGroupPfp(String chatId, PfpDTO pfpDTO) {
        ChatRoom chat = chatRoomRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        if(!chat.getIsGroup()) throw new RuntimeException("Chat is not a Group");
        chat.setPfpIndex(pfpDTO.getPfpIndex());
        chat.setPfpBg(pfpDTO.getPfpBg());

        ChatRoom savedChat = chatRoomRepo.save(chat);
        return new PfpDTO(savedChat.getPfpIndex(), savedChat.getPfpBg());
    }

    public Boolean changeAdmin(String admin, String candidate, String chatId) {
        ChatRoom chat = chatRoomRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        if(!chat.getIsGroup()) throw new RuntimeException("Chat is not a Group");
        if(!chat.getParticipantUsernames().contains(admin) && chat.getParticipantUsernames().contains(candidate) )
            throw new RuntimeException("User not part of " + chat.getChatName());
        if(!chat.getAdmin().getUsername().equals(admin)) throw new RuntimeException("You're not admin");
        User newAdmin = userRepo.findByUsername(candidate).orElseThrow();
        chat.setAdmin(newAdmin);
        chatRoomRepo.save(chat);
        return true;
    }

    public void deleteGroup(String chatId) {
        chatRoomRepo.deleteById(chatId);
    }

    public void deleteAllGroups(){
        for(ChatRoom chatRoom: chatRoomRepo.findAll()){
            if(chatRoom.getIsGroup()) chatRoomRepo.deleteById(chatRoom.getChatId());
        }
    }


}
