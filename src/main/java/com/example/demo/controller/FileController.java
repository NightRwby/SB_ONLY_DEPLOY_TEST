package com.example.demo.controller;

import com.example.demo.domain.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

    // 클라이언트 요청 예: /api/chat/download?storageKey=chat/uuid/file.png&fileName=원래_파일명.png
    @GetMapping("/api/chat/download")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam String storageKey, // 파일의 S3 키를 받습니다.
            @RequestParam(required = false) String fileName,
            Authentication authentication) {

        Resource resource;
        try {
            // 1. Service를 통해 S3에서 파일 스트림을 Resource 형태로 로드 (권한 확인 포함)
            resource = fileService.loadFileAsResource(storageKey);
        } catch (IOException e) {
            // S3 파일 로드 중 입출력 에러 발생 시 (500 에러)
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 전송 중 오류가 발생했습니다.", e);
        } catch (ResponseStatusException e) {
            // Service에서 던진 404, 403 에러 처리
            throw e;
        }

        // 2. 다운로드될 파일명 결정
        // fileName이 프론트에서 넘어온다면 사용하고, 없다면 S3 키에서 추출 (S3 키가 파일명을 포함하는 경우)
        String downloadFileName = (fileName != null && !fileName.isEmpty())
                ? fileName
                : resource.getFilename();

        // 3. HTTP 헤더 설정 (다운로드 강제 및 한글 파일명 인코딩)
        try {
            String contentType = "application/octet-stream";

            String encodedFileName = URLEncoder.encode(downloadFileName, "UTF-8").replaceAll("\\+", "%20");
            String contentDisposition = "attachment; filename*=UTF-8''" + encodedFileName;

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);

        } catch (UnsupportedEncodingException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}