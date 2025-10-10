package com.exotech.urchat.repository;

import com.exotech.urchat.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepo extends JpaRepository<Message, Long> {


    List<Message> findByChatRoomChatIdOrderByTimestampDesc(String chatId, Pageable pageable);

    List<Message> findByChatIdAndTimestampBeforeOrderByTimestampDesc(
            String chatId, LocalDateTime before, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.chatRoom.chatId = :chatId ORDER BY m.timestamp DESC LIMIT 1")
    Message findLastMessageByChatId(@Param("chatId") String chatId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom.chatId = :chatId AND m.timestamp > :since")
    Long countMessagesSince(@Param("chatId") String chatId, @Param("since") LocalDateTime since);

    @Modifying
    @Query("DELETE FROM Message m WHERE m.messageId = :messageId AND m.sender.username = :username")
    int deleteMessageByIdAndSender(@Param("messageId") Long messageId, @Param("username") String username);

    @Modifying
    @Query("DELETE FROM Message m WHERE m.chatRoom.chatId = :chatId")
    void deleteAllByChatId(@Param("chatId") String chatId);

    @Query("SELECT m FROM Message m WHERE m.chatRoom.chatId = :chatId AND m.sender.username = :sender ORDER BY m.timestamp DESC")
    List<Message> findUserMessagesInChat(@Param("chatId") String chatId, @Param("sender") String sender);

    @Query("SELECT m FROM Message m WHERE m.chatRoom.chatId = :chatId ORDER BY m.timestamp ASC")
    List<Message> findByChatIdOrderByTimestampAsc(@Param("chatId") String chatId);

    @Query("SELECT m FROM Message m JOIN FETCH m.sender JOIN FETCH m.chatRoom WHERE m.chatRoom.chatId = :chatId ORDER BY m.timestamp ASC")
    List<Message> findMessagesWithChatRoom(@Param("chatId") String chatId);

//    List<Message> findByChatId(String chatId);

    List<Message> findByChatRoom_ChatIdOrderByTimestampDesc(String chatId);

    List<Message> findByChatRoom_ChatId(String chatId);

    List<Message> findByChatRoom_ChatIdOrderByTimestampAsc(String chatId);

    @Query("SELECT m FROM Message m WHERE m.timestamp < :threshold")
    List<Message> findMessagesOlderThan(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query("DELETE FROM Message m WHERE m.timestamp < :threshold")
    int deleteMessagesOlderThan(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom.chatId = :chatId")
    long countByChatRoomChatId(@Param("chatId") String chatId);

    // Get messages with age information (for monitoring)
    @Query("SELECT m FROM Message m WHERE m.timestamp BETWEEN :start AND :end")
    List<Message> findMessagesBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.timestamp < :threshold")
    long countMessagesOlderThan(@Param("threshold") LocalDateTime threshold);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom.chatId = :chatId AND m.timestamp > :timestamp")
    long countByChatRoomChatIdAndTimestampAfter(@Param("chatId") String chatId, @Param("timestamp") LocalDateTime timestamp);
}
