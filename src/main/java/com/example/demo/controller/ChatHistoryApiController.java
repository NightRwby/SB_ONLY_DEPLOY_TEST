package com.example.demo.controller;

import com.example.demo.domain.dto.ChatMessageDto;
import com.example.demo.domain.service.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "채팅 기록 API", description = "과거 채팅 메시지 기록 조회 API")
public class ChatHistoryApiController {

    private final ChatMessageService chatMessageService;

    @GetMapping("/history/{roomId}")
    @Operation(summary = "채팅 기록 조회", description = "특정 방의 과거 채팅 메시지 기록을 시간순으로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = ChatMessageDto.class)))
    public ResponseEntity<List<ChatMessageDto>> getChatHistory(
            @Parameter(description = "채팅방 ID (String/UUID)", required = true)
            @PathVariable("roomId") String roomId) {

        List<ChatMessageDto> history = chatMessageService.findChatHistory(roomId);

        if (history.isEmpty()) {
            return ResponseEntity.ok(history);
        }

        return ResponseEntity.ok(history);
    }
}