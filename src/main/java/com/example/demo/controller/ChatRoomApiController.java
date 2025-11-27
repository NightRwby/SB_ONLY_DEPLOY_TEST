// com.example.demo.controller.ChatRoomApiController.java (유지 및 사용)

package com.example.demo.controller;

import com.example.demo.domain.dto.ChatRoomDto;
import com.example.demo.domain.dto.ChatRoomRequestDto;
import com.example.demo.domain.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/chat/room") // 기본 경로는 /api/chat/room
@RequiredArgsConstructor
public class ChatRoomApiController {

    private final ChatRoomService chatRoomService;

    // 1. 채팅방 생성 API (POST /api/chat/room)
    @PostMapping
    public ResponseEntity<ChatRoomDto> createRoom(
            @Valid @RequestBody ChatRoomRequestDto requestDto,
            Principal principal) {

        // 1. 인증 확인 및 사용자 이메일 추출
        if (principal == null || principal.getName().equals("anonymousUser")) {
            // 인증되지 않은 사용자 접근 방지
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        // 2. 인증된 사용자(Principal)의 이메일로 DTO의 CreatorId를 강제 설정
        String userEmail = principal.getName();
        requestDto.setCreatorId(userEmail);

        // 3. 서비스 호출 및 예외 처리
        try {
            ChatRoomDto newRoom = chatRoomService.createRoom(requestDto);
            return ResponseEntity.ok(newRoom);
        } catch (IllegalArgumentException e) {
            // Service에서 던진 "유저를 찾을 수 없습니다" 등의 비즈니스 예외는 400으로 처리
            // (이 부분은 Global Exception Handler를 통해 더 깔끔하게 처리할 수 있습니다.)
            return ResponseEntity.badRequest().body(null);
        }
    }

    // 2. 그룹 채팅방에 멤버를 추가하는 API (POST /api/chat/room/{roomId}/members)
    @PostMapping("/{roomId}/members")
    public ResponseEntity<Void> addMembersToRoom(
            @PathVariable("roomId") UUID roomId,
            @RequestBody List<String> memberIdentifiers
    ) {
        chatRoomService.addMembers(roomId, memberIdentifiers);
        return ResponseEntity.ok().build();
    }

    // 3. 그룹 채팅방의 현재 멤버 이메일 목록을 반환 (GET /api/chat/room/{roomId}/members/emails)
    @GetMapping("/{roomId}/members/emails")
    public ResponseEntity<List<String>> getRoomMemberEmails(@PathVariable("roomId") UUID roomId) {
        List<String> memberEmails = chatRoomService.getMemberEmails(roomId);
        return ResponseEntity.ok(memberEmails);
    }

    // 4. 특정 사용자 참여 채팅방 목록 조회 API (GET /api/chat/room/list/{userId})
    @GetMapping("/list/{userId}")
    public ResponseEntity<List<ChatRoomDto>> getChatRoomsByUserId(@PathVariable("userId") String userId) {
        List<ChatRoomDto> roomList = chatRoomService.getRoomsByUserId(userId);
        return ResponseEntity.ok(roomList);
    }

    // 5. 채팅방 삭제 API (DELETE /api/chat/room/{roomId})
    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable("roomId") UUID roomId) {
        chatRoomService.deleteRoom(roomId);
        return ResponseEntity.noContent().build();
    }

    // 6. 마지막 읽음 시간 갱신 API (POST /api/chat/room/{roomId}/read)
    @PostMapping("/{roomId}/read")
    public ResponseEntity<Void> updateLastReadTime(
            @PathVariable("roomId") String roomId,
            Principal principal
    ) {
        String userIdentifier = principal.getName();
        chatRoomService.updateLastReadTime(roomId, userIdentifier);
        return ResponseEntity.ok().build();
    }

    // 7. 1:1 채팅방 생성/조회 API (POST /api/chat/room/personal)
    @PostMapping("/personal")
    public ResponseEntity<ChatRoomDto> getOrCreatePersonalRoom(
            @RequestParam("targetEmail") String targetEmail, // 대상 유저의 Email
            Principal principal) {

        String myEmail = principal.getName();

        if (myEmail.equals(targetEmail)) {
            // 자기 자신과의 채팅방 생성은 BAD_REQUEST
            return ResponseEntity.badRequest().build();
        }

        // Service 호출 (존재하면 조회, 없으면 생성)
        ChatRoomDto room = chatRoomService.getOrCreatePersonalRoom(myEmail, targetEmail);

        return ResponseEntity.ok(room);
    }
}