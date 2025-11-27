package com.example.demo.controller;

import com.example.demo.domain.dto.ChatHistoryResponse;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.domain.dto.ChatRoomDto;
import com.example.demo.domain.dto.ChatRoomRequestDto;
import com.example.demo.domain.dto.PersonalRoomRequestDto;
import com.example.demo.domain.entity.user.User;
import com.example.demo.domain.service.ChatRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "채팅방 API", description = "채팅방 관리 API")
@Slf4j
public class RoomApiController {

    private final ChatRoomService chatRoomService;
    private final UserRepository userRepository;

    // 1. 채팅방 생성 (기존 유지)
    @PostMapping("/rooms")
    @Operation(summary = "채팅방 생성", description = "새로운 채팅방을 생성합니다. (JSON Body 사용)")
    @ApiResponse(responseCode = "200", description = "채팅방 생성 성공")
    public ChatRoomDto createChatRoom(
            @Valid @RequestBody ChatRoomRequestDto requestDto,
            Authentication authentication) {

        if (authentication == null || authentication.getName().equals("anonymousUser")) {
            throw new IllegalArgumentException("인증된 사용자 정보를 찾을 수 없습니다. (로그인 필요)");
        }

        String userEmail = authentication.getName();
        requestDto.setCreatorId(userEmail);

        ChatRoomDto newRoom = chatRoomService.createRoom(requestDto);
        return newRoom;
    }

    // 2. 채팅방 목록 조회 (기존 유지)
    @GetMapping("/rooms")
    @Operation(summary = "채팅방 목록 조회", description = "생성된 모든 채팅방 목록을 조회합니다.")
    public ResponseEntity<List<ChatRoomDto>> getAllRooms() {
        List<ChatRoomDto> rooms = chatRoomService.findAllRoom();
        return ResponseEntity.ok(rooms);
    }

    // 3. 채팅방 단건 조회 (기존 유지)
    @GetMapping("/rooms/{roomId}")
    @Operation(summary = "채팅방 조회", description = "roomId로 특정 채팅방 정보를 조회합니다.")
    public ResponseEntity<ChatRoomDto> getRoom(
            @Parameter(description = "채팅방 ID (String/UUID)", required = true)
            @PathVariable("roomId") String roomId) {

        ChatRoomDto room = chatRoomService.findRoomById(roomId);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(room);
    }

    // 4. 멤버 추가 (기존 유지)
    @PostMapping("/rooms/{roomId}/members")
    @Operation(summary = "채팅방 멤버 추가", description = "roomId와 이메일을 사용하여 해당 유저를 채팅방 멤버로 추가합니다.")
    public ResponseEntity<String> addMember(
            @PathVariable("roomId") String roomId,
            @RequestParam("email") String email) {

        try {
            String resultMessage = chatRoomService.addMemberToRoom(roomId, email);
            if (resultMessage.contains("존재하지 않습니다.")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resultMessage);
            }
            return ResponseEntity.ok(resultMessage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 5. [통합 & 수정] 1:1 채팅방 생성/조회 (JSON Body 방식 지원)
    @PostMapping("/rooms/personal")
    @Operation(summary = "1:1 채팅방 생성 또는 조회", description = "요청자와 대상 사용자 간의 1:1 채팅방을 생성하거나 조회합니다.")
    public ResponseEntity<ChatRoomDto> createOrGetPersonalRoom(
            @RequestBody PersonalRoomRequestDto request,
            Authentication authentication) {

        if (authentication == null || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String myIdFromAuth = authentication.getName();
        String friendId = request.getFriendId();

        // 서비스 메서드를 myIdFromAuth와 friendId로 호출
        ChatRoomDto room = chatRoomService.findPersonalRoom(myIdFromAuth, friendId);

        if (room != null) {
            return ResponseEntity.ok(room);
        }

        // 방이 없으면 생성
        ChatRoomDto newRoom = chatRoomService.createPersonalRoom(myIdFromAuth, friendId);
        return ResponseEntity.ok(newRoom);
    }

    // 6. 멤버 강퇴 (DELETE)
    @DeleteMapping("/rooms/{roomId}/members/{userEmail}") // PathVariable 이름도 email로 변경 권장
    @Operation(summary = "멤버 강퇴", description = "특정 채팅방에서 멤버를 강제 퇴장시킵니다.")
    public ResponseEntity<String> kickMember(
            @PathVariable String roomId,
            @PathVariable String userEmail, // Long 대신 String(이메일)로 변경
            Authentication authentication) {

        if (authentication == null || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        String kickerEmail = authentication.getName();

        try {
            User targetUser = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("강퇴할 사용자를 찾을 수 없습니다."));
            Long targetUserId = targetUser.getId();

            chatRoomService.kickMember(roomId, targetUserId, kickerEmail);

            return ResponseEntity.ok("멤버가 삭제되었습니다.");
        } catch (AccessDeniedException e) {
            log.error("강퇴 권한 없음: {}", e.getMessage());
            //권한 부족 시 403 Forbidden과 "권한이 없습니다." 메시지를 반환
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("권한이 없습니다.");
        } catch (IllegalArgumentException e) {
            log.warn("강퇴 실패: {}", e.getMessage());
            // 로직상 오류(예: 방장이 본인을 강퇴 시도) 시 400 Bad Request와 구체적인 메시지 반환
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("멤버 강퇴 중 서버 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
        }
    }

    // 7. [신규 추가] 마지막 읽은 시간 조회 API
    @GetMapping("/rooms/{roomId}/last-read-time")
    @Operation(summary = "마지막 읽은 시간 조회", description = "특정 채팅방에서 현재 로그인 사용자의 lastReadTime을 조회합니다. (ISO 8601 형식)")
    public ResponseEntity<String> getLastReadTime(
            @PathVariable String roomId,
            Authentication authentication) {

        if (authentication == null || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        String userEmail = authentication.getName();

        try {
            // ChatRoomService에서 구현한 메서드 호출
            String lastReadTime = chatRoomService.getLastReadTimeForUser(roomId, userEmail);
            log.info("LastReadTime 조회 완료. 반환 값: {}", lastReadTime);
            return ResponseEntity.ok(lastReadTime); // ISO 8601 문자열 반환 (예: "2025-11-23T00:00:00")
        } catch (IllegalArgumentException e) {
            log.warn("Last Read Time 조회 실패 (멤버 아님/방 없음): {}", e.getMessage());
            // 멤버가 아니거나 방이 없을 경우 빈 문자열 반환하여 클라이언트에서 맨 아래로 스크롤하게 함
            return ResponseEntity.ok("");
        } catch (Exception e) {
            log.error("Last Read Time 조회 중 서버 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류 발생");
        }
    }
    // 8. [신규 추가] 마지막 읽은 시간 갱신 API
    @PostMapping("/rooms/{roomId}/update-read-time")
    @Operation(summary = "마지막 읽은 시간 갱신", description = "특정 채팅방에서 현재 로그인 사용자의 lastReadTime을 현재 시각으로 갱신합니다.")
    public ResponseEntity<Void> updateLastReadTime(
            @PathVariable String roomId,
            Authentication authentication) {

        if (authentication == null || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userEmail = authentication.getName(); // 현재 로그인 사용자의 이메일 (ID) 획득

        try {
            // ChatRoomService에서 구현한 DB 갱신 메서드 호출
            chatRoomService.updateLastReadTime(roomId, userEmail);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Last Read Time 갱신 실패 (멤버 아님/방 없음): {}", e.getMessage());
            return ResponseEntity.notFound().build(); // 404 반환
        } catch (Exception e) {
            log.error("Last Read Time 갱신 중 서버 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 500 반환
        }
    }

    // 9. 채팅 이력 커서 기반 로딩 API
    @GetMapping("/rooms/{roomId}/messages/history")
    @Operation(summary = "채팅 이력 커서 기반 로딩",
            description = "lastReadTime을 기준으로 안 읽은 메시지와 초기 20개 메시지를 로드하거나, 커서를 기준으로 과거 메시지를 페이지 단위로 로드합니다.")
    public ResponseEntity<ChatHistoryResponse> loadChatHistory(
            @PathVariable String roomId,
            @Parameter(description = "이전 로드된 가장 오래된 메시지의 생성 시간 (ISO 8601)", example = "2025-11-20T10:00:00")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "사용자의 마지막 읽은 시간 (ISO 8601). 초기 로딩 시 사용.", example = "2025-11-24T09:37:58")
            @RequestParam(required = false) String lastReadTime,
            @Parameter(description = "로드할 메시지 개수 (상단 스크롤 시 50)", example = "50")
            @RequestParam(defaultValue = "50") int limit, // 기본값을 50으로 설정
            Authentication authentication) {

        if (authentication == null || authentication.getName().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userEmail = authentication.getName();

        try {
            // Service 호출
            // ChatRoomService에서 cursor가 null일 때 limit을 20으로 내부적으로 조정합니다.
            ChatHistoryResponse response = chatRoomService.loadChatHistory(roomId, cursor, lastReadTime, limit);

            return ResponseEntity.ok(response);

        } catch (AccessDeniedException e) {
            log.warn("채팅 이력 로딩 실패: 접근 권한 없음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            log.warn("채팅 이력 로딩 실패 (잘못된 요청): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("채팅 이력 로딩 중 서버 오류:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}