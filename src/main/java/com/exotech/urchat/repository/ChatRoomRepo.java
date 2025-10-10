package com.exotech.urchat.repository;

import com.exotech.urchat.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepo extends JpaRepository<ChatRoom, String> {

    @Query("SELECT DISTINCT cr FROM ChatRoom cr LEFT JOIN FETCH cr.participants WHERE cr.chatId = :chatId")
    Optional<ChatRoom> findByIdWithParticipants(@Param("chatId") String chatId);


    @Query("SELECT DISTINCT cr FROM ChatRoom cr JOIN cr.participants p WHERE p.username = :username")
    List<ChatRoom> findByParticipantUsername(@Param("username") String username);

    @Modifying
    @Query("DELETE FROM ChatRoom cr WHERE cr.chatId = :chatId")
    void deleteByChatId(@Param("chatId") String chatId);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.chatId = :chatId AND cr.admin.username = :username")
    Optional<ChatRoom> findByIdAndAdmin(@Param("chatId") String chatId, @Param("username") String username);

//    List<ChatRoom> findByParticipantUsernamesContaining(String username);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.isGroup = false AND " +
            "EXISTS (SELECT p1 FROM cr.participants p1 WHERE p1.username = :user1) AND " +
            "EXISTS (SELECT p2 FROM cr.participants p2 WHERE p2.username = :user2)")
    Optional<ChatRoom> findIndividualChat(@Param("user1") String user1, @Param("user2") String user2);

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.pendingInvitations pi WHERE pi.username = :username AND cr.isGroup = true")
    List<ChatRoom> findGroupInvitations(@Param("username") String username);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.isGroup = true AND cr.chatName ILIKE %:name%")
    List<ChatRoom> findGroupsByNameContaining(@Param("name") String searchName);

    @Query("SELECT COUNT(cr) > 0 FROM ChatRoom cr JOIN cr.participants p WHERE cr.chatId = :chatId AND p.username = :username")
    boolean existsByChatIdAndParticipantUsername(@Param("chatId") String chatId, @Param("username") String username);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.admin.username = :username AND cr.isGroup = true")
    List<ChatRoom> findGroupsByAdmin(@Param("username") String username);

//    boolean existsByChatIdAndParticipantUsernamesContaining(String chatId, String username);
}
