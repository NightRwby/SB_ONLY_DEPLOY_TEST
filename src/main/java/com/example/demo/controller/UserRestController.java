package com.example.demo.controller;

import com.example.demo.config.auth.PrincipalDetails;
import com.example.demo.domain.dto.ProfileResponseDto;
import com.example.demo.domain.dto.ProfileUpdateDto;
import com.example.demo.domain.service.FileService;
import com.example.demo.domain.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class UserRestController {

    private final UserService userService;
    private final FileService fileService;

    // 1. 내 프로필 정보 조회
    @GetMapping("/profile/me")
    public ResponseEntity<ProfileResponseDto> getMyProfile(
            @AuthenticationPrincipal PrincipalDetails principalDetails){
        if(principalDetails == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try{
            String email = principalDetails.getUsername();
            ProfileResponseDto profileData = userService.getProfileData(email);

            return ResponseEntity.ok(profileData);
        } catch (IllegalArgumentException e){
            log.error("프로필 조회 실패 : 사용자를 찾을 수 없음",e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // 2. 프로필 업데이트 (닉네임, 상태메시지, 사진 등)
    @PutMapping("/profile/update")
    public ResponseEntity<String> updateProfile(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @ModelAttribute ProfileUpdateDto dto) {

        if (principalDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        try {
            String email = principalDetails.getUsername();
            userService.updateProfile(email, dto);
            return ResponseEntity.ok("프로필이 성공적으로 업데이트되었습니다.");
        } catch (IOException e) {
            log.error("프로필 업데이트 중 파일 처리 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("파일처리 중 오류가 발생했습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 3. 프로필 배경 사진(Banner) 삭제
    @DeleteMapping("/profile/banner")
    public ResponseEntity<String> deleteBanner(
            @AuthenticationPrincipal PrincipalDetails principalDetails){

        if(principalDetails == null){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }
        try{
            String email = principalDetails.getUsername();
            userService.deleteBannerImage(email);

            return ResponseEntity.ok("배경이미지가 삭제되었습니다.");
        } catch(RuntimeException e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 4. 프로필 이미지 다운로드 (화면 표시용)
    // (FileController와 경로가 다르므로 유지합니다: /api/profile/download/**)
    @GetMapping("/profile/download/**")
    public ResponseEntity<Resource> downloadProfileImage(HttpServletRequest request){
        try {
            String requestURI = request.getRequestURI();
            String prefix = "/api/profile/download/";
            String storageKey = requestURI.substring(prefix.length());
            log.info("Extracted Storage Key : {}", storageKey);

            // FileService를 통해 S3 등에서 파일 로드
            Resource resource = fileService.loadFileAsResource(storageKey);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG) // 필요 시 동적 타입 적용 가능
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.noContent().build();
        }
    }
}