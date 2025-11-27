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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
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

    // DTO -> Entity 변환 메서드 (변경 없음)
    private ChatMessageEntity toEntity(ChatMessageDto dto) {
        return ChatMessageEntity.builder()
                .roomId(dto.getRoomId())
                .sender(dto.getSender())
                .senderName(dto.getSenderName())
                .senderInitial(dto.getSenderInitial())
                .message(dto.getMessage())
                .type(dto.getType())
                .fileUrl(dto.getFileUrl())
                .metadata(dto.getMetadata())
                .createdAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : LocalDateTime.now())
                .build();
    }

    // Entity -> DTO 변환 메서드
    private ChatMessageDto toDto(ChatMessageEntity entity) {

        String formattedTime = "";
        String isoTimeStr = ""; // ISO 시간 문자열 변수 추가

        if (entity.getCreatedAt() != null) {
            formattedTime = entity.getCreatedAt().format(TIME_FORMATTER); // 표시용
            // ISO 포맷터 사용
            isoTimeStr = entity.getCreatedAt().format(ISO_FORMATTER); // 비교용 (String vs String)
        }
        //log.info("formattedTime : "+formattedTime);
        //log.info("isoTimeStr : "+isoTimeStr);
        return ChatMessageDto.builder()
                .roomId(entity.getRoomId())
                .sender(entity.getSender())
                .senderName(entity.getSenderName())
                .senderInitial(entity.getSenderInitial())
                .message(entity.getMessage())
                .type(entity.getType())
                .fileUrl(entity.getFileUrl())
                .metadata(entity.getMetadata())
                .createdAt(entity.getCreatedAt())
                .isoTime(isoTimeStr)
                .time(formattedTime)
                .build();
    }

    /**
     * 메시지를 DB에 저장 (미디어 URL 및 메타데이터 처리 포함)
     */
    @Transactional
    public void saveMessage(ChatMessageDto messageDto) {
        if (messageDto.getCreatedAt() == null) {
            messageDto.setCreatedAt(LocalDateTime.now());
        }

        // ChatController에서 이미 senderName과 senderInitial이 채워진 DTO를 받았으므로,
        // 이를 엔티티로 변환하여 저장합니다.
        ChatMessageEntity entity = toEntity(messageDto);
        chatMessageRepository.save(entity);
    }

    /**
     * 특정 방의 채팅 기록 조회
     */
    public List<ChatMessageDto> findChatHistory(String roomId) {
        // DB에서 조회된 엔티티(senderName/Initial 포함)를 DTO로 변환하여 반환
        return chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // 커서 기반 메시지 조회 로직
    // ------------------------------------------------------------------

    /**
     * lastReadTime을 기준으로 초기 메시지를 로드하거나, 커서를 기준으로 과거 메시지를 페이지네이션 로드합니다.
     * @param roomId 채팅방 ID
     * @param cursor 현재 로드된 가장 오래된 메시지의 생성 시간 (ISO String), 초기 로딩 시 null
     * @param lastReadTimeStr 사용자의 마지막 읽은 시간 (ISO String). 초기 로딩 시 사용.
     * @param limit 한 번에 로드할 메시지 개수 (상단 스크롤 시 50)
     * @return ChatHistoryResponse
     */
    @Transactional(readOnly = true)
    public ChatHistoryResponse loadChatHistory(String roomId, String cursor, String lastReadTimeStr, int limit) {

        LocalDateTime lastReadTime = toLocalDateTime(lastReadTimeStr);
        LocalDateTime cursorTime = toLocalDateTime(cursor);

        List<ChatMessageEntity> resultEntities;
        boolean hasMore;

        // 1. 초기 로딩 (cursor가 null)
        if (cursorTime == null) {
            // [A] 초기 로딩 로직: lastReadTime 기준으로 안 읽은 메시지 + 과거 20개 로드

            // lastReadTime이 없으면 현재 시각으로 설정하여 모든 메시지를 과거 메시지로 처리 (선택적)
            if (lastReadTime == null) {
                lastReadTime = LocalDateTime.now();
            }

            // 1. lastReadTime 이후의 안 읽은 메시지 모두 조회 (오름차순: 오래된 순)
            List<ChatMessageEntity> unreadMessages = chatMessageRepository.findByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(roomId, lastReadTime);

            // 2. lastReadTime 이전의 과거 메시지 20개 조회 (내림차순: 최신이 위)
            int pastLimit = 20; // 초기 로딩 과거 메시지 개수
            Pageable pastPageable = PageRequest.of(0, pastLimit);
            // lastReadTime보다 과거 시점 메시지 중, 최신순(내림차순)으로 20개 조회
            List<ChatMessageEntity> pastMessagesDesc = new ArrayList<>(
                    chatMessageRepository.findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(roomId, lastReadTime, pastPageable).getContent()
            );
            // 3. 순서를 다시 오름차순으로 바꿔 병합
            Collections.reverse(pastMessagesDesc);

            // 최종 리스트: 과거 20개 (오래된 순) -> 안 읽은 메시지 (오래된 순)
            resultEntities = Stream.concat(pastMessagesDesc.stream(), unreadMessages.stream())
                    .collect(Collectors.toList());

            // 4. 추가 과거 메시지 존재 여부 확인
            if (pastMessagesDesc.isEmpty()) {
                hasMore = false;
            } else {
                // 20개 메시지 중 가장 오래된 메시지(첫 번째)의 시간보다 더 과거 메시지가 있는지 확인
                LocalDateTime oldestTime = pastMessagesDesc.get(0).getCreatedAt();

                // oldestTime이 null인 경우 처리
                if (oldestTime == null) {
                    log.warn("[WARN] Oldest message in pastMessagesDesc has a null createdAt. Cannot determine hasMore.");
                    hasMore = false;
                } else {
                    hasMore = chatMessageRepository.existsByRoomIdAndCreatedAtBefore(roomId, oldestTime);
                }
            }

        } else {
            // [B] 상단 스크롤 로딩: cursor 기준 과거 limit(50)개 로드
            Pageable pastPageable = PageRequest.of(0, limit);
            Page<ChatMessageEntity> pastPage = chatMessageRepository.findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(roomId, cursorTime, pastPageable);

            List<ChatMessageEntity> pastMessagesDesc = new ArrayList<>(pastPage.getContent());

            hasMore = pastPage.hasContent() && pastPage.getTotalElements() > limit;

            // 라인 179 부근의 remove() 호출이 있다면 이제 정상적으로 작동합니다.
            // 예: pastMessagesDesc.remove(pastMessagesDesc.size() - 1); // 또는 remove(0) 등

            // 역순으로 가져온 메시지를 다시 오름차순으로 정렬
            Collections.reverse(pastMessagesDesc);

            resultEntities = pastMessagesDesc;
        }

        // DTO 변환
        List<ChatMessageDto> messagesDto = resultEntities.stream()
                .map(this::toDto) // 기존 toDto 사용
                .collect(Collectors.toList());

        // 다음 커서 설정 (로드된 메시지 중 가장 오래된 메시지의 isoTime String)
        String nextCursor = messagesDto.isEmpty() ? null : messagesDto.get(0).getIsoTime(); // isoTime 사용

        return ChatHistoryResponse.builder()
                .messages(messagesDto)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    /**
     * 헬퍼 메서드: ISO String을 LocalDateTime으로 변환
     */
    private LocalDateTime toLocalDateTime(String isoString) {
        if (isoString == null || isoString.isEmpty()) {
            return null;
        }
        try {
            // String 문자열에 SSSSSS가 포함되어 있으면 해당 포맷터를 사용
            return LocalDateTime.parse(isoString, ISO_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("LocalDateTime 파싱 오류: {} - 기본 파서 시도", isoString);
            try {
                // SSSSSS가 없는 경우를 대비한 기본 파서 시도
                return LocalDateTime.parse(isoString);
            } catch (DateTimeParseException e2) {
                log.error("LocalDateTime 파싱 최종 실패: {}", isoString);
                return null;
            }
        }
    }
}