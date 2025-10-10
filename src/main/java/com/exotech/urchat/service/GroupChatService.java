package com.exotech.urchat.service;

import com.exotech.urchat.dto.messageDTOs.MessageDTOConvertor;
import com.exotech.urchat.dto.chatDTOs.*;
import com.exotech.urchat.model.ChatRoom;
import com.exotech.urchat.model.User;
import com.exotech.urchat.repository.ChatRoomRepo;
import com.exotech.urchat.repository.MessageRepo;
import com.exotech.urchat.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupChatService {

    private final ChatRoomRepo chatRoomRepo;
    private final MessageRepo messageRepo;
    private final UserRepo userRepo;
    private final MessageDTOConvertor messageDTOConvertor;
    private final ChatDTOConvertor chatDTOConvertor;
    private final ChatService chatService;

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

        for (String username : participantUsernames) {
            if (!username.equals(adminUsername)) {
                User user = userRepo.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User " + username + " not found"));
                group.getPendingInvitations().add(user);

                pendingMembersDTO.add(chatDTOConvertor.convertToGroupMembersDTO(user, false, false));
            }
        }

        ChatRoom chat =  chatRoomRepo.save(group);
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
        ChatRoom chat = chatRoomRepo.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!chat.hasParticipant(username)) {
            throw new RuntimeException("You are not a member of this group");
        }

        chat.getParticipants().removeIf(participant -> participant.getUsername().equals(username));

        if (chat.getAdmin().getUsername().equals(username)) {
            if (!chat.getParticipants().isEmpty()) {
                User newAdmin = chat.getParticipants().get(0);
                chat.setAdmin(newAdmin);
            } else {
                chatRoomRepo.delete(chat);
                return;
            }
        }

        chatRoomRepo.save(chat);
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

    public void deleteGroup(String chatId) {
        chatRoomRepo.deleteById(chatId);
    }

    public void deleteAllGroups(){
        for(ChatRoom chatRoom: chatRoomRepo.findAll()){
            if(chatRoom.getIsGroup()) chatRoomRepo.deleteById(chatRoom.getChatId());
        }
    }
}
