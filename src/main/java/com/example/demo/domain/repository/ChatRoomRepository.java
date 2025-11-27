package com.example.demo.domain.repository;

import com.example.demo.domain.entity.ChatRoomEntity;
import com.example.demo.domain.entity.user.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {
    Optional<ChatRoomEntity> findByRoomId(String roomId);

    @Query("SELECT r FROM ChatRoomEntity r " +
            "JOIN r.roomMembers rm1 " +
            "JOIN r.roomMembers rm2 " +
            "WHERE r.type = 'PERSONAL' " +
            "AND rm1.user = :userA AND rm2.user = :userB")
    Optional<ChatRoomEntity> findPersonalRoomByMembers(@Param("userA") User userA, @Param("userB") User userB);
}