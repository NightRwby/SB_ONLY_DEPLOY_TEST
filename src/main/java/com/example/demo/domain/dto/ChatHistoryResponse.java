package com.example.demo.domain.dto;

import com.example.demo.domain.dto.ChatMessageDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryResponse {

    // 로드된 메시지 리스트
    private List<ChatMessageDto> messages;

    // 새로 로드된 메시지 중 가장 오래된 메시지의 createdAt (다음 커서로 사용)
    private String nextCursor;

    // 더 과거의 메시지가 남아 있는지 여부 (상단에 생략 영역을 표시할지 결정)
    private boolean hasMore;
}