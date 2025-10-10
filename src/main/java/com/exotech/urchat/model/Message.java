package com.exotech.urchat.model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "messages")
public class Message{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId;

    @Column(length = 1000)
    private String messageContent;

    @ManyToOne()
    @JoinColumn(name = "sender_username", nullable = false)
    private User sender;

    @ManyToOne()
    @JoinColumn(name = "chat_id", nullable = false)
    private ChatRoom chatRoom;

    @CreationTimestamp
    private LocalDateTime timestamp;

    public String getSenderUsername() {
        return sender != null ? sender.getUsername() : null;
    }

    public String getChatId() {
        return chatRoom != null ? chatRoom.getChatId() : null;
    }
}