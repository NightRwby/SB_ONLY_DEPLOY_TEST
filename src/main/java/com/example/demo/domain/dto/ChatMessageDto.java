package com.example.demo.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "채팅 메시지")
@Builder
public class ChatMessageDto {

    public enum ChatType {
        GROUP, // 그룹 채팅
        FRIEND // 1:1 친구 채팅
    }

    @Schema(description = "메시지 타입", example = "TALK")
    public enum MessageType {
        @Schema(description = "채팅방 참여")
        ENTER,
        @Schema(description = "대화 나누기")
        TALK,
        @Schema(description = "채팅방 퇴장")
        QUIT,
        // 미디어 및 링크 타입
        @Schema(description = "이미지 전송")
        IMAGE,
        @Schema(description = "파일 전송")
        FILE,
        @Schema(description = "URL 링크")
        URL_LINK
    }

    @Schema(description = "메시지 고유 ID (수정/삭제 식별용)", example = "152")
    private Long messageId;

    @Schema(description = "메시지 타입", example = "TALK", required = true)
    private MessageType type;

    @Schema(description = "채팅방 종류", example = "GROUP", required = true)
    private ChatType chatType;

    @Schema(description = "채팅방 ID", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    private String roomId;

    @Schema(description = "메시지 보낸 사람", example = "홍길동", required = true)
    private String sender;

    // 발신자 이름 (프론트엔드 senderName 대체)
    @Schema(description = "메시지 보낸 사람의 표시 이름", example = "홍길동", required = true)
    private String senderName;

    // 발신자 초성 (프론트엔드 senderInitial 대체)
    @Schema(description = "메시지 보낸 사람의 초성", example = "ㅎㄱㄷ", required = true)
    private String senderInitial;

    // 텍스트 메시지 내용 (IMAGE/FILE/URL_LINK 타입일 경우 캡션 또는 빈 값)
    @Schema(description = "메시지 내용 (텍스트 또는 캡션)", example = "안녕하세요!")
    private String message;

    // 이미지/파일/링크의 실제 URL 또는 저장 경로
    @Schema(description = "미디어 파일/링크 URL (IMAGE/FILE/URL_LINK 타입에 사용)", example = "https://example.com/images/123.jpg")
    private String fileUrl;

    // 파일명, 파일 크기 등 메타데이터 저장용
    @Schema(description = "파일 이름, 파일 크기 등 메타데이터 (FILE 타입에 주로 사용)", example = "{\"fileName\": \"여행사진.jpg\", \"fileSize\": 102400}")
    private String metadata;

    @Schema(description = "프론트엔드 표시용 시간", example = "오전 10:30")
    private String time;

    private String isoTime;

    @JsonProperty("createdAt")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
    private LocalDateTime createdAt;
}