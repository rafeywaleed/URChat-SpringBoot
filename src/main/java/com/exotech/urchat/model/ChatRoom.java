package com.exotech.urchat.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Entity
@Table(name = "chat_rooms")
public class ChatRoom {

    @Id
    private String chatId;

    private String chatName;

    private Boolean isGroup;

    private String pfpIndex;
    private String pfpBg;

    private LocalDateTime lastActivity;
    private String lastMessage;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "chat_room_participants",
            joinColumns = @JoinColumn(name = "chat_id"),
            inverseJoinColumns = @JoinColumn(name = "username")
    )
    @ToString.Exclude
    private List<User> participants = new ArrayList<>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "chat_room_pending_invitations",
            joinColumns = @JoinColumn(name = "chat_id"),
            inverseJoinColumns = @JoinColumn(name = "username")
    )
    @ToString.Exclude
    private List<User> pendingInvitations = new ArrayList<>();

    @ManyToOne()
    @JoinColumn(name = "admin_username")
    @ToString.Exclude
    private User admin;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Message> messages = new ArrayList<>();

    private Integer themeIndex= 0;

    private Boolean isDarkTheme = true;

    public String getDisplayName(String currentUsername) {
        if (isGroup) {
            return chatName != null ? chatName : admin.getUsername() + "'s Group";
        } else {
            return participants.stream()
                    .filter(user -> !user.getUsername().equals(currentUsername))
                    .findFirst()
                    .map(User::getUsername)
                    .orElse("Unknown");
        }
    }

    public String getChatPfpIndex(String currentUsername) {
        if (isGroup) {
            return pfpIndex != null && !pfpIndex.equals("👥") ? pfpIndex : "👥";
        } else {
            return participants.stream()
                    .filter(user -> !user.getUsername().equals(currentUsername))
                    .findFirst()
                    .map(User::getPfpIndex)
                    .orElse("💬");
        }
    }

    public String getChatPfpBg(String currentUsername) {
        if (isGroup) {
            return pfpBg != null && !pfpBg.equals("#4CAF50") ? pfpBg : "#4CAF50";
        } else {
            return participants.stream()
                    .filter(user -> !user.getUsername().equals(currentUsername))
                    .findFirst()
                    .map(User::getPfpBg)
                    .orElse("#2196F3");
        }
    }



    public List<String> getParticipantUsernames() {
        return participants.stream()
                .map(User::getUsername)
                .toList();
    }

    public List<String> getRequestedUsernames() {
        return pendingInvitations.stream()
                .map(User::getUsername)
                .toList();
    }

    public boolean hasParticipant(String username) {
        return participants.stream()
                .anyMatch(user -> user.getUsername().equals(username));
    }

    public boolean hasPendingInvitation(String username) {
        return pendingInvitations.stream()
                .anyMatch(user -> user.getUsername().equals(username));
    }

    @Override
    public String toString() {
        return "ChatRoom{" +
                "chatId='" + chatId + '\'' +
                ", chatName='" + chatName + '\'' +
                ", isGroup=" + isGroup +
                ", pfpIndex='" + pfpIndex + '\'' +
                ", pfpBg='" + pfpBg + '\'' +
                ", lastActivity=" + lastActivity +
                '}';
    }

    @Transient
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

}
