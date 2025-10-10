package com.exotech.urchat.repository;

import com.exotech.urchat.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByRefreshToken(String refreshToken);

    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT(:username, '%'))")
    List<User> findByUsernameStartingWith(@Param("username") String username);

    @Query("SELECT u FROM User u WHERE u.username IN :usernames")
    List<User> findAllByUsernames(@Param("usernames") List<String> usernames);

    @Query("SELECT u FROM User u JOIN u.chatRoomInvitations cri WHERE cri.chatId = :chatId")
    List<User> findUsersWithPendingInvitation(@Param("chatId") String chatId);
}
