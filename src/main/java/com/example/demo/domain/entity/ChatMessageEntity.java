package com.example.demo.domain.entity;

import com.example.demo.domain.dto.ChatMessageDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "message")
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageId; // messageId long pk

    @Column(nullable = false)
    private String roomId;

    @Column(nullable = false)
    private String sender;

    @Column(nullable = true)
    private String senderName;

    @Column(nullable = true)
    private String senderInitial;

    // 텍스트 메시지 또는 미디어의 캡션/설명 저장용
    @Column(columnDefinition = "TEXT", nullable = true)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatMessageDto.MessageType type; // type text notNull

    // 이미지/파일/링크의 실제 URL 또는 저장 경로
    @Column(columnDefinition = "VARCHAR(500)", nullable = true)
    private String fileUrl;

    // 파일명, 파일 크기 등 미디어 메타데이터 저장용
    @Column(columnDefinition = "TEXT", nullable = true)
    private String metadata;

    @Column(nullable = false)
    private LocalDateTime createdAt; // creatAt Datetime now()

    @Builder
    public ChatMessageEntity(
            String roomId,
            String sender,
            String senderName,
            String senderInitial,
            String message,
            ChatMessageDto.MessageType type,
            LocalDateTime createdAt,
            String fileUrl,
            String metadata)
    {
        this.roomId = roomId;
        this.sender = sender;
        this.senderName = senderName;
        this.senderInitial = senderInitial;
        this.message = message;
        this.type = type;
        this.createdAt =  createdAt;
        this.fileUrl = fileUrl;
        this.metadata = metadata;
    }

    // 메시지 내용 수정을 위한 메서드
    public void setMessage(String message) {
        this.message = message;
    }
}