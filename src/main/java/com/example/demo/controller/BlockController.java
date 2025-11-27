package com.example.demo.controller;

import com.example.demo.config.auth.PrincipalDetails;
import com.example.demo.domain.dto.inputDto.BlockRequest;
import com.example.demo.domain.dto.outputDto.BlockResponse;
import com.example.demo.domain.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // REST API Controller임을 명시
@RequestMapping("/api") // 기본 경로를 /api로 설정
@RequiredArgsConstructor
@Slf4j
public class BlockController {

    // 차단 관련 로직은 UserService에 구현되어 있으므로 주입받습니다.
    private final UserService userService;

    // 1. 차단 요청 (POST /api/block)
    // 클라이언트: blockUser(targetUserEmail)
    @PostMapping("/block")
    public ResponseEntity<?> blockUser(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestBody BlockRequest blockRequest) {

        String userEmail = principalDetails.getEmail();
        log.info("차단 요청: 사용자 {}가 {}를 차단 시도", userEmail, blockRequest.getTargetUserEmail());

        try {
            userService.blockUser(userEmail, blockRequest);
            return ResponseEntity.ok("사용자 차단 성공");
        } catch (Exception e) {
            log.error("차단 실패: {}", e.getMessage());
            // 차단 요청 실패 시 클라이언트 JS가 에러 메시지를 띄울 수 있도록 400 Bad Request와 메시지를 반환
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // 2. 차단 해제 요청 (DELETE /api/block)
    // 클라이언트: unblockUser(targetUserEmail) / handleClickUnblock(email)
    @DeleteMapping("/block")
    public ResponseEntity<?> unblockUser(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestBody BlockRequest blockRequest) {

        String userEmail = principalDetails.getEmail();
        log.info("차단 해제 요청: 사용자 {}가 {}를 차단 해제 시도", userEmail, blockRequest.getTargetUserEmail());

        try {
            userService.unblockUser(userEmail, blockRequest);
            return ResponseEntity.ok("사용자 차단 해제 성공");
        } catch (IllegalArgumentException e) {
            log.warn("차단 해제 실패 (기록 없음): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("차단 해제 실패 (서버 오류): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("차단 해제 중 서버 오류 발생");
        }
    }

    // 3. 차단 목록 조회 (GET /api/blocklist)
    // 클라이언트: fetchAndRenderBlockList()
    @GetMapping("/blocklist")
    public ResponseEntity<List<BlockResponse>> getBlockList(
            @AuthenticationPrincipal PrincipalDetails principalDetails) {

        String userEmail = principalDetails.getEmail();
        log.info("차단 목록 조회 요청: 사용자 {}", userEmail);

        try {
            List<BlockResponse> blockList = userService.getBlockedUsers(userEmail);
            return ResponseEntity.ok(blockList);
        } catch (Exception e) {
            log.error("차단 목록 조회 실패: {}", e.getMessage());
            // API 로드 실패 시 500 Internal Server Error 반환
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}