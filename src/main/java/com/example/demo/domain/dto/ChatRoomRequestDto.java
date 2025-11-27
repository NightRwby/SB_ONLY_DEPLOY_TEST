package com.example.demo.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ChatRoomRequestDto {

    @NotBlank(message = "방 이름은 필수입니다.")
    private String roomName;

    @NotBlank(message = "생성자 ID는 필수입니다.")
    private String creatorId; // 클라이언트의 myProfile.userId가 들어옴

    @NotBlank(message = "방 타입은 필수입니다.")
    private String roomType; // GROUP, PERSONAL 등

    @NotNull(message = "참여자 목록은 필수입니다.")
    private List<String> memberIds;
}