// src/main/java/com/example/demo/domain/dto/outputDto/ChatRoomDto.java (경로 가정)

package com.example.demo.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "채팅방 정보")
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDto {

    @Schema(description = "채팅방 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String roomId;

    @Schema(description = "채팅방 이름", example = "우리들의 채팅방")
    private String roomName;

    // 채팅방 타입 필드 추가
    @Schema(description = "채팅방 타입 (GROUP, PERSONAL 등)", example = "GROUP")
    private String type; // RoomType Enum을 String으로 변환하여 전달

    @Schema(description = "현재 참여 인원수", example = "3")
    private int memberCount;

    @Schema(description = "사용자가 아직 읽지 않은 메시지 개수")
    private long unreadCount;

    // 마지막 메시지 필드 추가
    @Schema(description = "마지막 메시지 내용", example = "안녕하세요!")
    private String lastMessage;

    // 마지막 메시지 시간 필드 추가
    @Schema(description = "마지막 메시지 전송 시각 (ISO-8601 포맷)", example = "2025-11-19T10:30:00")
    private LocalDateTime lastMessageTime;
}