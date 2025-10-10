package com.exotech.urchat.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "username"),
        @UniqueConstraint(columnNames = "email")
})
public class User {

    @Id
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 25)
    private String fullName;

    @Column(length = 100)
    private String bio;

    @Builder.Default
    private String pfpIndex = "😊";
    @Builder.Default
    private String pfpBg = "#4CAF50";

    @CreationTimestamp
    private LocalDateTime joinedAt;

    @ManyToMany(mappedBy = "participants")
    @ToString.Exclude
    private List<ChatRoom> chatRooms = new ArrayList<>();


    @ManyToMany(mappedBy = "pendingInvitations")
    @ToString.Exclude
    private List<ChatRoom> chatRoomInvitations = new ArrayList<>();

//    @OneToMany(mappedBy = "sender", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
//    private List<Message> messages = new ArrayList<>();

//    @OneToMany(mappedBy = "admin")
//    @ToString.Exclude
//    private List<ChatRoom> adminChatRooms = new ArrayList<>();

    private String refreshToken;
    private LocalDateTime refreshTokenExpiry;

    public boolean isInChatRoom(String chatId) {
        return chatRooms.stream().anyMatch(chat -> chat.getChatId().equals(chatId));
    }

    public boolean hasPendingInvitation(String chatId) {
        return chatRoomInvitations.stream().anyMatch(chat -> chat.getChatId().equals(chatId));
    }

    public void setInitalPfpIndex(String emoji){
        if(pfpIndex==null || emoji == null || pfpIndex.equals("😊")){
            int hash = username.hashCode();
            String[] emojiPalette = {
                    "😊", "😂", "🥰", "😎", "🤩", "🧐", "😋", "🤠",
                    "😍", "🥳", "🤖", "👻", "🐱", "🐶", "🦊", "🐼"
            };
            int index = Math.abs(hash) % emojiPalette.length;
            pfpIndex = emojiPalette[index];
        }
        else pfpIndex  = emoji;
    }

    public void setInitialPfpBg(String color){
        if(pfpBg==null || color == null || pfpBg.equals("#4CAF50")){
            int hash = username.hashCode();
            String[] colorPalette = {
                    "#2196F3", "#FF9800", "#F44336", "#9C27B0",
                    "#673AB7", "#3F51B5", "#00BCD4", "#009688", "#8BC34A",
                    "#FFC107", "#FF5722", "#795548", "#607D8B", "#E91E63"
            };
            int index = Math.abs(hash) % colorPalette.length;
            pfpBg = colorPalette[index];
        }
        else pfpBg  = color;
    }



    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", bio='" + bio + '\'' +
                ", pfpIndex='" + pfpIndex + '\'' +
                ", pfpBg='" + pfpBg + '\'' +
                ", joinedAt=" + joinedAt +
                '}';
    }
}
