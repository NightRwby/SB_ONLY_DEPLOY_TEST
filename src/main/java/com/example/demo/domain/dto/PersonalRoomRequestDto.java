package com.example.demo.domain.dto;

import lombok.Data;

@Data
public class PersonalRoomRequestDto {
    private String myId;     // 내 식별자 (Email 또는 ID)
    private String friendId; // 친구 식별자 (Email 또는 ID)
    private String friendName; // 친구 이름
}