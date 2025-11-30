package com.example.demo.controller;

import com.example.demo.domain.dto.ChatMessageDto;
import com.example.demo.domain.dto.FileUrlResponse;
import com.example.demo.domain.entity.user.User;
import com.example.demo.domain.enums.RoomType;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.domain.service.ChatMessageService;
import com.example.demo.domain.service.ChatRoomService;
import com.example.demo.domain.service.FileService;
import com.example.demo.util.KoreanNameUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/chat")
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMessageService chatMessageService;
    private final ChatRoomService chatRoomService;
    private final FileService fileService;
    private final UserRepository userRepository;

    // 채팅방 생성 요청 DTO 정의
    @Data
    private static class ChatRoomCreateRequest {
        private String name;
        private RoomType type;
    }

    // 1. 참여자 목록 조회 REST API (GET /api/chat/participants/{roomId})
    @GetMapping("/participants/{roomId}")
    public ResponseEntity<List<?>> getParticipants(@PathVariable String roomId) {
        List<?> participants = chatRoomService.getParticipants(roomId);
        return ResponseEntity.ok(participants);
    }

    // 2. 파일 업로드 REST API (POST /api/chat/upload)
    @PostMapping("/upload")
    public ResponseEntity<FileUrlResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("roomId") String roomId,
            @RequestParam("senderEmail") String senderEmail) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            FileUrlResponse response = fileService.uploadFile(file, "chat/" + roomId);

            ChatMessageDto fileMessage = ChatMessageDto.builder()
                    .roomId(roomId)
                    .type(ChatMessageDto.MessageType.FILE)
                    .sender(senderEmail)
                    .message(response.getStorageKey() + "|" + response.getOriginalFileName())
                    .createdAt(LocalDateTime.now())
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[ERROR] File upload failed: {}", e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    // ----------------------------------------------------------------------------------
    // 파일 다운로드 REST API (GET /api/chat/download/{storageKey})
    // ----------------------------------------------------------------------------------
    @GetMapping("/download/**")
    public ResponseEntity<Resource> downloadFile(
            HttpServletRequest request,
            @RequestParam(value = "fileName", required = false) String originalFileName) {

        String requestUri = request.getRequestURI();
        String storageKey = requestUri.substring(requestUri.indexOf("/download/") + 10);
        storageKey = java.net.URLDecoder.decode(storageKey, StandardCharsets.UTF_8);

        log.info("[API] File Download Request: StorageKey={}", storageKey);

        Resource resource;
        try {
            resource = fileService.loadFileAsResource(storageKey);
        } catch (IOException e) {
            log.error("[ERROR] File I/O error for key {}: {}", storageKey, e.getMessage());
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "파일 전송 중 오류가 발생했습니다.", e);
        }

        String displayFileName;
        if (originalFileName != null && !originalFileName.isEmpty()) {
            displayFileName = originalFileName;
        } else {
            displayFileName = storageKey.substring(storageKey.lastIndexOf("/") + 1);
        }

        String encodedFileName = UriUtils.encode(displayFileName, StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"");
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    // ----------------------------------------------------------------------------------
    // 메시지 삭제 API (DELETE /api/chat/message/{messageId})
    // ----------------------------------------------------------------------------------
    @DeleteMapping("/message/{messageId}")
    public ResponseEntity<String> deleteMessage(@PathVariable Long messageId) {
        try {
            chatMessageService.deleteMessage(messageId);
            return ResponseEntity.ok("메시지가 삭제되었습니다.");
        } catch (Exception e) {
            log.error("메시지 삭제 실패: ID={}, Error={}", messageId, e.getMessage());
            return ResponseEntity.badRequest().body("삭제 실패: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------------------------
    // 메시지 수정 API (PUT /api/chat/message/{messageId})
    // ----------------------------------------------------------------------------------
    @PutMapping("/message/{messageId}")
    public ResponseEntity<String> updateMessage(@PathVariable Long messageId, @RequestBody Map<String, String> payload) {
        try {
            String newMessage = payload.get("message");
            if (newMessage == null || newMessage.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("수정할 메시지 내용이 비어있습니다.");
            }
            chatMessageService.updateMessage(messageId, newMessage);
            return ResponseEntity.ok("메시지가 수정되었습니다.");
        } catch (Exception e) {
            log.error("메시지 수정 실패: ID={}, Error={}", messageId, e.getMessage());
            return ResponseEntity.badRequest().body("수정 실패: " + e.getMessage());
        }
    }

    // 3. STOMP WebSocket 메시지 처리 (경로: /pub/chat/message)
    @MessageMapping("/chat/message")
    public void message(ChatMessageDto message) {
        String roomId = message.getRoomId();
        message.setCreatedAt(LocalDateTime.now());
        log.info("[CHAT_DEBUG] message: {}", message);

        ChatMessageDto.MessageType messageType = message.getType();
        log.info("[CHAT_DEBUG] Received message from sender: {}", message.getSender());

        if (messageType == ChatMessageDto.MessageType.TALK ||
                messageType == ChatMessageDto.MessageType.IMAGE ||
                messageType == ChatMessageDto.MessageType.FILE ||
                messageType == ChatMessageDto.MessageType.URL_LINK) {
            log.info("[CHAT_DEBUG] MessageType"+messageType);
            Optional<User> senderUserOpt = userRepository.findByEmail(message.getSender());
            if (senderUserOpt.isEmpty()) {
                try {
                    Long userId = Long.parseLong(message.getSender());
                    senderUserOpt = userRepository.findById(userId);
                    if (senderUserOpt.isPresent()) {
                        log.info("[CHAT_DEBUG] User found successfully by ID: {}", userId);
                    }
                } catch (NumberFormatException e) {
                }
            }
            if (senderUserOpt.isPresent()) {
                User senderUser = senderUserOpt.get();
                String senderName = senderUser.getUserName();
                String senderInitial = KoreanNameUtil.extractInitial(senderName);

                log.info("[CHAT_DEBUG] Found user info. Name: {}, Initial: {}", senderName, senderInitial);

                message.setSenderName(senderName);
                message.setSenderInitial(senderInitial);

            } else {
                log.warn("[CHAT_WARN] User lookup failed for sender (Final Attempt): {}", message.getSender());
                message.setSenderName("알 수 없음");
                message.setSenderInitial("?");
            }
        }

        if (ChatMessageDto.MessageType.ENTER.equals(messageType)) {
            message.setMessage(message.getSender() + "님이 채팅방에 참여했습니다.");
            chatMessageService.saveMessage(message);
        } else if (ChatMessageDto.MessageType.QUIT.equals(messageType)) {
            message.setMessage(message.getSender() + "님이 채팅방에서 나갔습니다.");
            chatMessageService.saveMessage(message);
        } else if (ChatMessageDto.MessageType.TALK.equals(messageType) ||
                ChatMessageDto.MessageType.IMAGE.equals(messageType) ||
                ChatMessageDto.MessageType.FILE.equals(messageType) ||
                ChatMessageDto.MessageType.URL_LINK.equals(messageType)) {

            chatMessageService.saveMessage(message);
        }

        messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, message);
    }
}