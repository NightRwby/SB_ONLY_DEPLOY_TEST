package com.example.demo.controller;

import com.example.demo.domain.dto.UserDto;
import com.example.demo.domain.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
@Tag(name = "친구 API", description = "친구 관리 API")
@Slf4j
public class FriendController {

    private final FriendService friendService;

    /**
     * 친구 목록 조회
     */
    @GetMapping("/list")
    @Operation(summary = "친구 목록 조회", description = "현재 로그인된 사용자의 친구 목록을 조회합니다.")
    public ResponseEntity<List<UserDto>> getFriendsList(Authentication authentication) {
        String userEmail = authentication.getName();
        List<UserDto> friends = friendService.getFriendsList(userEmail);
        return ResponseEntity.ok(friends);
    }

    /**
     * 사용자 검색
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserDto>> searchUsers(@RequestParam String query, Authentication authentication) {
        String userEmail = authentication.getName();

        List<UserDto> users = friendService.searchUsersForFriendRequest(query, userEmail);
        return ResponseEntity.ok(users);
    }


    /**
     * 친구 요청 전송
     */
    @PostMapping("/request")
    @Operation(summary = "친구 요청 전송", description = "특정 사용자에게 친구 요청을 보냅니다.")
    public ResponseEntity<Map<String, String>> sendFriendRequest(
            @RequestBody Map<String, String> requestBody, // 👈 여기를 수정했습니다.
            Authentication authentication) {
        try {
            String senderEmail = authentication.getName();
            String receiverEmail = requestBody.get("receiverEmail");

            friendService.sendFriendRequest(senderEmail, receiverEmail);
            return ResponseEntity.ok(Map.of("message", "친구 요청이 전송되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 받은 친구 요청 목록 조회
     */
    @GetMapping("/requests/received")
    @Operation(summary = "받은 친구 요청 목록", description = "현재 로그인된 사용자가 받은 친구 요청 목록을 조회합니다.")
    public ResponseEntity<List<UserDto>> getReceivedRequests(Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("@@@@@@@@@userEmail : "+userEmail);
        List<UserDto> requests = friendService.getReceivedRequests(userEmail);
        log.info("@@@@@@@@@requests : "+requests);
        return ResponseEntity.ok(requests);
    }

    /**
     * 친구 요청 수락
     */
    @PostMapping("/request/accept")
    @Operation(summary = "친구 요청 수락", description = "친구 요청을 수락합니다.")
    public ResponseEntity<Map<String, String>> acceptFriendRequest(
            @RequestParam String senderEmail,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            friendService.acceptFriendRequest(userEmail, senderEmail);
            return ResponseEntity.ok(Map.of("message", "친구 요청이 수락되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 친구 요청 거절
     */
    @PostMapping("/request/reject")
    @Operation(summary = "친구 요청 거절", description = "친구 요청을 거절합니다.")
    public ResponseEntity<Map<String, String>> rejectFriendRequest(
            @RequestParam String senderEmail,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            friendService.rejectFriendRequest(userEmail, senderEmail);
            return ResponseEntity.ok(Map.of("message", "친구 요청이 거절되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 친구 삭제
     */
    @DeleteMapping("/delete")
    @Operation(summary = "친구 삭제", description = "친구 관계를 삭제합니다.")
    public ResponseEntity<Map<String, String>> deleteFriend(
            @RequestParam String friendEmail,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            friendService.deleteFriend(userEmail, friendEmail);
            return ResponseEntity.ok(Map.of("message", "친구가 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    /**
     * 친구 차단 (친구 삭제 포함)
     */
    @PostMapping("/block")
    public ResponseEntity<Map<String, String>> blockFriend(
            @RequestParam String blockedEmail,
            Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            friendService.blockUserAndRemoveFriend(userEmail, blockedEmail);
            return ResponseEntity.ok(Map.of("message", "사용자를 차단했습니다."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}