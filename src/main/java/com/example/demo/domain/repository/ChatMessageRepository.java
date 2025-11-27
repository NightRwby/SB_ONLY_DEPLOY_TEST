// src/main/java/com/example/demo/domain/repository/ChatMessageRepository.java

package com.example.demo.domain.repository;

import com.example.demo.domain.entity.ChatMessageEntity;
import org.springframework.data.domain.Page; // ⭐ 추가
import org.springframework.data.domain.Pageable; // ⭐ 추가
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    // 특정 방의 모든 메시지를 오래된 순으로 조회
    List<ChatMessageEntity> findByRoomIdOrderByCreatedAtAsc(String roomId);

    // -----------------------------------------------------------------
    // 커서 기반 페이지네이션을 위한 메서드
    // -----------------------------------------------------------------
    /**
     * 1. 안 읽은 메시지 조회: 특정 시간(lastReadTime) 이후의 메시지를 생성 시간 오름차순으로 모두 조회 (오래된 순)
     */
    List<ChatMessageEntity> findByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(String roomId, LocalDateTime createdAt);

    /**
     * 2. 과거 메시지 조회: 특정 시간(cursor) 이전의 메시지를 생성 시간 내림차순(최신 순)으로 페이지 단위 조회
     */
    Page<ChatMessageEntity> findByRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(String roomId, LocalDateTime createdAt, Pageable pageable);

    /**
     * 3. 추가 과거 메시지 존재 여부 확인: 특정 시간 이전의 메시지가 하나라도 있는지 확인
     */
    boolean existsByRoomIdAndCreatedAtBefore(String roomId, LocalDateTime createdAt);

    long countByRoomIdAndCreatedAtAfter(String roomId, LocalDateTime lastReadTime);

    long countByRoomIdAndCreatedAtGreaterThanEqual(String roomId, LocalDateTime time);

    // RoomId로 찾고, CreatedAt을 내림차순(Desc)으로 정렬하여, 맨 위(Top)의 1개만 Optional로 반환합니다.
    Optional<ChatMessageEntity> findTopByRoomIdOrderByCreatedAtDesc(String roomId);
}