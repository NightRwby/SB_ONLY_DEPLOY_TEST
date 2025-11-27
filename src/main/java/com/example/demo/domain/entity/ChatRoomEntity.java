package com.example.demo.domain.entity;

import com.example.demo.domain.enums.RoomType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "room")
public class ChatRoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Long PK (자동 생성) - roomId 대신 사용

    @Column(nullable = false)
    private String roomId; // UUID 형태의 클라이언트용 ID

    @Column(nullable = false)
    private String roomName; // roomName (text Notnull)

    @Column(nullable = false)
    private String roomOwner; // roomOwner (text Notnull) <- 이메일(ID)값 저장

    // RoomType 필드 추가
    @Enumerated(EnumType.STRING) // DB에 Enum 이름을 문자열로 저장
    @Column(nullable = false)
    private RoomType type;

    // RoomMemberEntity와 1:N 관계 설정
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomMemberEntity> roomMembers = new ArrayList<>();


    //현재 인원수
    public int getUserCount() {
        return this.roomMembers.size();
    }

    // RoomMemberEntity 추가 (RoomMemberEntity에서 room을 설정함)
    public void addRoomMember(RoomMemberEntity roomMember) {
        this.roomMembers.add(roomMember);
    }

    // Builder에 type 필드 추가
    @Builder
    public ChatRoomEntity(String roomId, String roomName, String roomOwner, RoomType type) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.roomOwner = roomOwner;
        this.type = type;
    }
}