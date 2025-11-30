package com.example.demo.domain.service;

import com.example.demo.domain.dto.ChatHistoryResponse;
import com.example.demo.domain.dto.ChatMessageDto;
import com.example.demo.domain.entity.ChatMessageEntity;
import com.example.demo.domain.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("a h:mm", Locale.KOREA);

    // DTO -> Entity 변환 메서드
    private ChatMessageEntity toEntity(ChatMessageDto dto) {
        return ChatMessageEntity.builder()
                .roomId(dto.getRoomId())
                .sender(dto.getSender())
                .senderName(dto.getSenderName())
                .senderInitial(dto.getSenderInitial())
                .message(dto.getMessage())
                .type(dto.getType())
                // 파일 및 메타데이터 매핑 (두 코드 모두 포함됨)
                .fileUrl(dto.getFileUrl())
                .metadata(dto.getMetadata())
                .createdAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : LocalDateTime.now())
                .build();
    }

    // Entity -> DTO 변환 메서드
    private ChatMessageDto toDto(ChatMessageEntity entity) {

        String formattedTime = "";
        String isoTimeStr = "";

        if (entity.getCreatedAt() != null) {
            formattedTime = entity.getCreatedAt().format(TIME_FORMATTER);
            isoTimeStr = entity.getCreatedAt().format(ISO_FORMATTER);
        }

        return ChatMessageDto.builder()
                .messageId(entity.getMessageId()) // 🔑 [중요] 삭제/수정을 위한 ID 매핑
                .roomId(entity.getRoomId())
                .sender(entity.getSender())
                .senderName(entity.getSenderName())
                .senderInitial(entity.getSenderInitial())
                .message(entity.getMessage())
                .type(entity.getType())
                // 파일 및 메타데이터 매핑
                .fileUrl(entity.getFileUrl())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .isoTime(isoTimeStr)     // 프론트엔드 정렬/비교용
                .time(formattedTime)     // 프론트엔드 표시용
                .build();
    }

    /**
     * 메시지를 DB에 저장
     */
    @Transactional
    public void saveMessage(ChatMessageDto messageDto) {
        if (messageDto.getCreatedAt() == null) {
            messageDto.setCreatedAt(LocalDateTime.now());
        }
        ChatMessageEntity entity = toEntity(messageDto);
        chatMessageRepository.save(entity);
    }

    // ==========================================================
    // 메시지 삭제 및 수정 로직 (첫 번째 코드 기능 유지)
    // ==========================================================

    /**
     * 메시지 삭제
     */
    @Transactional
    public void deleteMessage(Long messageId) {
        if (chatMessageRepository.existsById(messageId)) {
            chatMessageRepository.deleteById(messageId);
            log.info("메시지 삭제 완료: ID={}", messageId);
        } else {
            throw new IllegalArgumentException("해당 메시지가 존재하지 않습니다. ID=" + messageId);
        }
    }

    /**
     * 메시지 수정
     */
    @Transactional
    public void updateMessage(Long messageId, String newMessage) {
        ChatMessageEntity messageEntity = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("해당 메시지가 존재하지 않습니다. ID=" + messageId));

        // Dirty Checking으로 자동 업데이트
        messageEntity.setMessage(newMessage);
        log.info("메시지 수정 완료: ID={}", messageId);
    }

    // ==========================================================

    /**
     * 특정 방의 채팅 기록 조회 (단순 리스트)
     */
    public List<ChatMessageDto> findChatHistory(String roomId) {
        return chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 커서 기반 메시지 조회 로직 (페이지네이션)
     */
    @Transactional(readOnly = true)
    public ChatHistoryResponse loadChatHistory(String roomId, String cursor, String lastReadTimeStr, int limit) {

        LocalDateTime lastReadTime = toLocalDateTime(lastReadTimeStr);
        LocalDateTime cursorTime = toLocalDateTime(cursor);

        List<ChatMessageEntity> resultEntities;
        boolean hasMore;

        // 1. 초기 로딩 (cursor가 null)
        if (cursorTime == null) {
            if (lastReadTime == null) {
                lastReadTime = LocalDateTime.now();
            }

            // 안 읽은 메시지 (오름차순)
            List<ChatMessageEntity> unreadMessages = chatMessageRepository.findByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(roomId, lastReadTime);

            // 과거 메시지 (내림차순 조회 후 뒤집기)
            int pastLimit = 20;
            Pageable pastPageable = PageRequest.of(0, pastLimit);
            List<ChatMessageEntity> pastMessagesDesc = new ArrayList<>(
                    chatMessageRepository.findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(roomId, lastReadTime, pastPageable).getContent()
            );
            Collections.reverse(pastMessagesDesc);

            resultEntities = Stream.concat(pastMessagesDesc.stream(), unreadMessages.stream())
                    .collect(Collectors.toList());

            if (pastMessagesDesc.isEmpty()) {
                hasMore = false;
            } else {
                LocalDateTime oldestTime = pastMessagesDesc.get(0).getCreatedAt();
                if (oldestTime == null) {
                    hasMore = false;
                } else {
                    hasMore = chatMessageRepository.existsByRoomIdAndCreatedAtBefore(roomId, oldestTime);
                }
            }

        } else {
            // 2. 상단 스크롤 추가 로딩
            Pageable pastPageable = PageRequest.of(0, limit);
            Page<ChatMessageEntity> pastPage = chatMessageRepository.findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(roomId, cursorTime, pastPageable);

            List<ChatMessageEntity> pastMessagesDesc = new ArrayList<>(pastPage.getContent());
            hasMore = pastPage.hasContent() && pastPage.getTotalElements() > limit;
            Collections.reverse(pastMessagesDesc);

            resultEntities = pastMessagesDesc;
        }

        List<ChatMessageDto> messagesDto = resultEntities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        String nextCursor = messagesDto.isEmpty() ? null : messagesDto.get(0).getIsoTime();

        return ChatHistoryResponse.builder()
                .messages(messagesDto)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    private LocalDateTime toLocalDateTime(String isoString) {
        if (isoString == null || isoString.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(isoString, ISO_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("LocalDateTime 파싱 오류: {} - 기본 파서 시도", isoString);
            try {
                return LocalDateTime.parse(isoString);
            } catch (DateTimeParseException e2) {
                log.error("LocalDateTime 파싱 최종 실패: {}", isoString);
                return null;
            }
        }
    }
}