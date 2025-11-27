package com.example.demo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUrlResponse {
    private String originalFileName; // 클라이언트가 업로드한 원래 이름
    private String storageKey;       // S3에 저장된 고유 경로 (다운로드 API의 매개변수)
    private String fileUrl;          // 브라우저에서 직접 접근 가능한 URL (선택적)
}