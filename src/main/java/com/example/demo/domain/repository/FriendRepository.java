package com.example.demo.domain.repository;

import com.example.demo.domain.entity.FriendEntity;
import com.example.demo.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<FriendEntity, Long> {
    
    // 특정 유저의 친구 목록 조회
    List<FriendEntity> findByUser(User user);
    
    // 친구 관계 존재 여부 확인
    boolean existsByUserAndFriend(User user, User friend);
    
    // 특정 친구 관계 조회
    Optional<FriendEntity> findByUserAndFriend(User user, User friend);
    
    // 양방향 친구 관계 확인 (A-B 또는 B-A)
    @Query("SELECT f FROM FriendEntity f WHERE " +
           "(f.user = :user1 AND f.friend = :user2) OR " +
           "(f.user = :user2 AND f.friend = :user1)")
    Optional<FriendEntity> findFriendshipBetween(
        @Param("user1") User user1, 
        @Param("user2") User user2
    );
    
    // 친구 수 카운트
    long countByUser(User user);
}
