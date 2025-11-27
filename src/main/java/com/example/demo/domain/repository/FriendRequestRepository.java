package com.example.demo.domain.repository;

import com.example.demo.domain.entity.FriendRequestEntity;
import com.example.demo.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FriendRequestRepository extends JpaRepository<FriendRequestEntity, Long> {
    
    // 받은 요청 목록 (PENDING 상태만)
    List<FriendRequestEntity> findByReceiverAndStatus(
        User receiver, 
        FriendRequestEntity.RequestStatus status
    );
    
    // 보낸 요청 목록
    List<FriendRequestEntity> findBySenderAndStatus(
        User sender, 
        FriendRequestEntity.RequestStatus status
    );
    
    // 특정 요청 조회
    Optional<FriendRequestEntity> findBySenderAndReceiverAndStatus(
        User sender, 
        User receiver, 
        FriendRequestEntity.RequestStatus status
    );
    
    // 요청 존재 여부 확인 (중복 방지)
    boolean existsBySenderAndReceiverAndStatus(
        User sender, 
        User receiver, 
        FriendRequestEntity.RequestStatus status
    );
}
