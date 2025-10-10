package com.exotech.urchat.dto.chatDTOs;

import com.exotech.urchat.model.ChatRoom;
import com.exotech.urchat.model.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChatDTOConvertor {

    public ChatRoomDTO convertToChatRoomDTO(ChatRoom chatRoom) {
        return ChatRoomDTO.builder()
                .chatId(chatRoom.getChatId())
                .chatName(chatRoom.getChatName())
                .isGroup(chatRoom.getIsGroup())
                .lastActivity(chatRoom.getLastActivity())
                .lastMessage(chatRoom.getLastMessage())
                .pfpIndex(chatRoom.getPfpIndex())
                .pfpBg(chatRoom.getPfpBg())
                .themeIndex(chatRoom.getThemeIndex() == null ? 0 : chatRoom.getThemeIndex())
                .isDark(chatRoom.getIsDarkTheme() == null )
                .build();
    }

    public UserChatRoomDTO convertToUserChatRoomDTO(ChatRoom chatRoom, User otherUser) {
        return new UserChatRoomDTO(
                chatRoom.getChatName(),
                otherUser.getFullName(),
                chatRoom.getChatId(),
                chatRoom.getIsGroup(),
                otherUser.getBio(),
                otherUser.getPfpIndex(),
                otherUser.getPfpBg()
        );
    }

    public GroupMembersDTO convertToGroupMembersDTO(User user, boolean isAdmin, boolean isMember) {
        return new GroupMembersDTO(
                user.getUsername(),
                user.getFullName(),
                user.getPfpIndex(),
                user.getPfpBg(),
                isAdmin,
                isMember
        );
    }

    public GroupChatRoomDTO convertToGroupChatRoomDTO(ChatRoom chatRoom,
                                                      String admin,
                                                      List<GroupMembersDTO> groupMembers,
                                                      List<GroupMembersDTO> memberRequests) {
        return new GroupChatRoomDTO(
                chatRoom.getChatName(),
                chatRoom.getChatId(),
                chatRoom.getIsGroup(),
                admin,
                groupMembers,
                memberRequests,
                chatRoom.getPfpIndex(),
                chatRoom.getPfpBg()
        );
    }

    public List<GroupMembersDTO> mapUsersToGroupMembersDTO(List<User> users, String adminUsername) {
        return users.stream()
                .map(user -> convertToGroupMembersDTO(
                        user,
                        adminUsername.equals(user.getUsername()),
                        true // they're members
                ))
                .collect(Collectors.toList());
    }

}
