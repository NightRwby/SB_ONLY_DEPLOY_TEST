package com.example.demo.domain.entity;

import com.example.demo.domain.dto.RoomMemberId;
import com.example.demo.domain.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "room_member")
@IdClass(RoomMemberId.class)
public class RoomMemberEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("room")
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoomEntity room;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("user")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 방에 참여한 시간
    @Column(nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();

    // 새로운 멤버가 추가될 때 joinedAt과 동일하게 현재 시각으로 초기화합니다.
    private LocalDateTime lastReadTime = LocalDateTime.now();

    @Builder
    public RoomMemberEntity(ChatRoomEntity room, User user) {
        this.room = room;
        this.user = user;
        this.joinedAt = LocalDateTime.now();
        this.lastReadTime = LocalDateTime.now();
    }
}