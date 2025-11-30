package com.example.demo.controller;

import com.example.demo.domain.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@RestController
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    // 1. 파일 다운로드 API
    // 요청: /api/chat/download?storageKey=...&fileName=...
    @GetMapping("/api/chat/download")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam String storageKey,
            @RequestParam(required = false) String fileName,
            Authentication authentication) {

        Resource resource;
        try {
            // S3에서 파일 로드
            resource = fileService.loadFileAsResource(storageKey);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 전송 중 오류가 발생했습니다.", e);
        }

        // 다운로드 파일명 설정
        String downloadFileName = (fileName != null && !fileName.isEmpty())
                ? fileName
                : resource.getFilename();

        try {
            // 한글 파일명 깨짐 방지 인코딩
            String encodedFileName = URLEncoder.encode(downloadFileName, "UTF-8").replaceAll("\\+", "%20");
            String contentDisposition = "attachment; filename*=UTF-8''" + encodedFileName;

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM) // 바이너리 데이터로 처리
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);

        } catch (UnsupportedEncodingException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 2. 프로필/배너 이미지 조회 API (화면 표시용 - 인라인)
    @GetMapping("/download/{storageKey}")
    public ResponseEntity<Resource> downloadProfileImage(@PathVariable String storageKey) {
        Resource resource;
        try {
            resource = fileService.loadFileAsResource(storageKey);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 로드 중 오류가 발생했습니다.", e);
        }

        // 이미지 타입에 맞는 Content-Type 설정 (브라우저가 이미지를 바로 보여주도록 함)
        String contentType = determineContentType(storageKey);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    // MIME 타입 결정 헬퍼 메서드
    private String determineContentType(String storageKey) {
        String lowerKey = storageKey.toLowerCase();
        if (lowerKey.endsWith(".png")) return "image/png";
        if (lowerKey.endsWith(".jpg") || lowerKey.endsWith(".jpeg")) return "image/jpeg";
        if (lowerKey.endsWith(".gif")) return "image/gif";
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}