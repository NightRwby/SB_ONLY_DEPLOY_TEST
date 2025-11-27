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
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
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
        // ... (기존 getParticipants 로직 유지) ...
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
            // 1. FileService를 통해 S3에 파일 저장 및 DTO 반환
            FileUrlResponse response = fileService.uploadFile(file, "chat/" + roomId);

            // 2. 파일 업로드 메시지를 채팅방에 브로드캐스팅 및 DB 저장
            //     이때 message 필드에는 storageKey와 originalFileName을 JSON 형태로 담는 것이 일반적입니다.
            ChatMessageDto fileMessage = ChatMessageDto.builder()
                    .roomId(roomId)
                    .type(ChatMessageDto.MessageType.FILE)
                    .sender(senderEmail) // S3 업로드를 요청한 사용자
                    .message(response.getStorageKey() + "|" + response.getOriginalFileName()) // 간단하게 키|파일명 형태로 저장
                    .createdAt(LocalDateTime.now())
                    .build();

            // 사용자 정보 보강 및 메시지 저장
            // 이 로직은 기존 ChatController의 message() 메서드 내부 로직을 가져와야 합니다.
            // 여기서는 간략히 표현합니다.
            // chatMessageService.saveAndBroadcast(fileMessage);

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

        // 1. 요청 URL에서 "/download/" 뒷부분의 경로(StorageKey)를 통째로 추출
        String requestUri = request.getRequestURI();
        String storageKey = requestUri.substring(requestUri.indexOf("/download/") + 10);

        // 2. URL 디코딩 (한글 파일명이나 공백 처리 등을 위해 필수)
        storageKey = java.net.URLDecoder.decode(storageKey, StandardCharsets.UTF_8);

        log.info("[API] File Download Request: StorageKey={}", storageKey);

        // 3. 서비스 호출 (기존 로직 유지)
        Resource resource;
        try {
            resource = fileService.loadFileAsResource(storageKey);
        } catch (IOException e) {
            log.error("[ERROR] File I/O error for key {}: {}", storageKey, e.getMessage());
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "파일 전송 중 오류가 발생했습니다.", e);
        }

        // 4. 파일명 설정 (기존 로직 유지)
        String displayFileName;
        if (originalFileName != null && !originalFileName.isEmpty()) {
            displayFileName = originalFileName;
        } else {
            displayFileName = storageKey.substring(storageKey.lastIndexOf("/") + 1);
        }

        // 5. 헤더 설정 및 응답 반환 (기존 로직 유지)
        String encodedFileName = UriUtils.encode(displayFileName, StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"");
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    // 3. STOMP WebSocket 메시지 처리 (경로: /pub/chat/message)
    @MessageMapping("/chat/message")
    public void message(ChatMessageDto message) {
        String roomId = message.getRoomId();
        message.setCreatedAt(LocalDateTime.now());
        log.info("[CHAT_DEBUG] message: {}", message);

        ChatMessageDto.MessageType messageType = message.getType();
        log.info("[CHAT_DEBUG] Received message from sender: {}", message.getSender());
        // TALK, IMAGE, FILE, URL_LINK 등 실제 유저 메시지 유형에 대해서만 사용자 정보를 조회하고 DTO에 채웁니다.
        if (messageType == ChatMessageDto.MessageType.TALK ||
                messageType == ChatMessageDto.MessageType.IMAGE ||
                messageType == ChatMessageDto.MessageType.FILE ||
                messageType == ChatMessageDto.MessageType.URL_LINK) {
            log.info("[CHAT_DEBUG] MessageType"+messageType);
            // DTO의 sender 필드(이메일)를 사용하여 User 조회
            Optional<User> senderUserOpt = userRepository.findByEmail(message.getSender());
            if (senderUserOpt.isEmpty()) {
                try {
                    Long userId = Long.parseLong(message.getSender());
                    senderUserOpt = userRepository.findById(userId);
                    if (senderUserOpt.isPresent()) {
                        log.info("[CHAT_DEBUG] User found successfully by ID: {}", userId);
                    }
                } catch (NumberFormatException e) {
                    // sender가 Long으로 파싱되지 않으면, ID로 찾는 시도 종료
                }
            }
            if (senderUserOpt.isPresent()) {
                User senderUser = senderUserOpt.get();
                String senderName = senderUser.getUserName();
                String senderInitial = KoreanNameUtil.extractInitial(senderName);

                log.info("[CHAT_DEBUG] Found user info. Name: {}, Initial: {}", senderName, senderInitial);

                // DTO에 이름과 초성 정보 주입
                message.setSenderName(senderName);
                message.setSenderInitial(senderInitial);

            } else {
                log.warn("[CHAT_WARN] User lookup failed for sender (Final Attempt): {}", message.getSender());
                // 찾을 수 없는 경우 기본값 설정 (프론트엔드에서 처리할 수 있도록)
                message.setSenderName("알 수 없음");
                message.setSenderInitial("?");
            }
        }
        // ----------------------------------------------------

        // 메시지 저장 로직
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

            // DTO에 senderName과 senderInitial이 채워진 상태로 저장 및 브로드캐스팅됨
            chatMessageService.saveMessage(message);
        }

        // WebSocket으로 브로드캐스팅 (발신자 정보가 보강된 DTO가 전송됨)
        messagingTemplate.convertAndSend("/sub/chat/room/" + roomId, message);
    }
}