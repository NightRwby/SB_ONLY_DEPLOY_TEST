package com.example.demo.domain.repository;

import com.example.demo.domain.dto.RoomMemberId;
import com.example.demo.domain.entity.ChatRoomEntity;
import com.example.demo.domain.entity.RoomMemberEntity;
import com.example.demo.domain.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMemberEntity, RoomMemberId> {

    // 특정 방에 특정 유저가 존재하는지 확인하는 메서드
    Optional<RoomMemberEntity> findByRoomAndUser(ChatRoomEntity room, User user);

    // 특정 채팅방의 모든 멤버 엔티티 목록을 조회
    List<RoomMemberEntity> findByRoom(ChatRoomEntity room);

    List<RoomMemberEntity> findByUser(User user);
}
