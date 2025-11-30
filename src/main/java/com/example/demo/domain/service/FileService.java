package com.example.demo.domain.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.example.demo.domain.dto.FileUrlResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import com.example.demo.test.LocalFileUtil; // LocalFileUtil 임포트

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final AmazonS3 s3Client;
    private final String s3BucketName;
    private final ObjectMapper objectMapper;
    private final StorageModeManager storageModeManager;

    // 파일이 실제로 저장되는 베이스 URL (S3 연동 시 https://{bucket}.s3.region.amazonaws.com/ 등)
    private final String STORAGE_BASE_URL = "https://your-storage-domain.com/";

    // =========================================================================
    // 1. 파일 다운로드 (Public API)
    // =========================================================================
    public Resource loadFileAsResource(String storageKey) throws IOException {

        // 🚨 [수정] 조건부 로직만 남기고 S3 try-catch 블록 제거
        if (storageModeManager.isLocalMode()) {
            log.warn("DOWNLOADING IN LOCAL MODE: {}", storageKey);
            return LocalFileUtil.loadFromLocal(storageKey);
        } else {
            log.info("DOWNLOADING IN S3 MODE: {}", storageKey);
            return loadFromS3(storageKey);
        }
    }

    // -------------------------------------------------------------------------
    // S3 다운로드 헬퍼 메서드 (S3 로직 집합)
    // -------------------------------------------------------------------------
    private Resource loadFromS3(String storageKey) throws IOException {
        try {
            // 2. 🔑 [핵심] S3에서 파일 데이터를 InputStream으로 읽어오기
            S3Object s3Object = s3Client.getObject(new GetObjectRequest(s3BucketName, storageKey));
            InputStream inputStream = s3Object.getObjectContent();

            // 3. InputStream을 Resource로 감싸서 반환
            return new InputStreamResource(inputStream);

        } catch (AmazonS3Exception e) {
            // S3 자체에서 발생하는 에러 (404 Not Found, 403 Forbidden 등) 처리
            log.error("S3 파일 로드 중 오류 발생: {}", storageKey, e);
            if (e.getStatusCode() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "S3에 파일이 존재하지 않습니다.", e);
            } else if (e.getStatusCode() == 403) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "S3 접근이 거부되었습니다. 권한을 확인해주세요.", e);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 파일 로드 중 오류 발생", e);

        } catch (Exception e) {
            // 기타 I/O 및 런타임 에러 처리
            log.error("파일 로드 중 예상치 못한 오류 발생: {}", storageKey, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 전송 중 알 수 없는 오류 발생", e);
        }
    }


    // =========================================================================
    // 2. 파일 업로드 (Public API)
    // =========================================================================
    public FileUrlResponse uploadFile(MultipartFile file, String dirPath) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String storageKey; // 🔑 storageKey는 여기서 선언하고 할당합니다.

        // 🚨 [수정] 조건부 로직만 남김
        if (storageModeManager.isLocalMode()) {
            log.warn("UPLOADING IN LOCAL MODE: {}", originalFilename);
            storageKey = LocalFileUtil.saveToLocal(file, dirPath);
        } else {
            log.info("UPLOADING IN S3 MODE: {}", originalFilename);
            storageKey = saveToS3(file, dirPath); // S3 헬퍼 메서드 호출
        }

        // 4. 저장된 파일의 URL 생성 (공통)
        String fileUrl = "/api/chat/download/" + storageKey;

        // 5. DB에 저장할 정보 반환 (공통)
        return FileUrlResponse.builder()
                .originalFileName(originalFilename)
                .storageKey(storageKey)
                .fileUrl(fileUrl)
                .build();
    }

    // -------------------------------------------------------------------------
    // S3 업로드 헬퍼 메서드 (S3 로직 집합)
    // -------------------------------------------------------------------------
    private String saveToS3(MultipartFile file, String dirPath) throws IOException {
        String originalFilename = file.getOriginalFilename();

        // 1. 저장할 고유 키 (Storage Key) 생성
        String uuid = UUID.randomUUID().toString();
        String extension = determineFileType(originalFilename);
        String storageKey = dirPath + "/" + uuid + "." + extension; // 🔑 S3 Key 생성

        // 2. 파일 메타데이터 설정 (필수)
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        try (InputStream inputStream = file.getInputStream()) {
            // 3. 🔑 [핵심] S3에 파일 업로드 요청
            s3Client.putObject(new PutObjectRequest(
                    s3BucketName,
                    storageKey,
                    inputStream,
                    metadata
            ));
        } catch (Exception e) {
            log.error("S3 파일 업로드 실패: {}", storageKey, e);
            throw new IOException("파일 업로드 중 S3 오류 발생", e);
        }

        return storageKey;
    }

    // -------------------------------------------------------------------------
    //  프로필 업데이트를 위한 헬퍼 메서드 추가
    // -------------------------------------------------------------------------

    public String uploadAndReturnStorageKey(MultipartFile file,String dirPath){
        String originalFilename = file.getOriginalFilename();
        String storageKey = null;

        if (storageModeManager.isLocalMode()){
            log.warn("UPLOADING PROFILE IN LOCAL MODE: {}", originalFilename);
            storageKey = LocalFileUtil.saveToLocal(file, dirPath);
        }else {
            log.info("UPLOADING PROFILE IN S3 MODE: {}", originalFilename);
            try {
                storageKey = saveToS3(file, dirPath);
            } catch (Exception e) {
                log.error("S3 파일 업로드 중 오류 발생: {}", originalFilename, e);
            }
        }

        log.info("DEBUG: FileService returned final storageKey: {}", storageKey);

        if (storageKey == null || storageKey.isEmpty()) {
            log.error("ERROR: FileService가 파일을 저장하지 못하고 NULL 또는 빈 키를 반환했습니다.");
        }

        return storageKey;
    }


    // -------------------------------------------------------------------------
    // 프로필 이미지 삭제 메서드
    // -------------------------------------------------------------------------

    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return; // 삭제할 URL이 없으면 즉시 종료
        }

        // 1. URL에서 Storage Key 추출
        // 예: /api/profile/download/profile/uuid.jpg -> profile/uuid.jpg
        // 여기서는 간단하게 URL의 마지막 부분(StorageKey)을 추출한다고 가정합니다.
        String storageKey = extractStorageKey(fileUrl);

        if (storageModeManager.isLocalMode()) {
            // 🚨 Local Mode: LocalFileUtil 사용
            LocalFileUtil.deleteFromLocal(storageKey);
        } else {
            // 🚨 S3 Mode: S3 삭제 헬퍼 메서드 사용 (구현 필요)
            // deleteFromS3(storageKey);
        }
    }

    private String extractStorageKey(String fileUrl){
        String prefix ="/api/profile/download";
        if(fileUrl.startsWith(prefix)){
            return fileUrl.substring(prefix.length());
        }
        return fileUrl;
    }


    // -------------------------------------------------------------------------
    // 확장자 추출 메서드
    // -------------------------------------------------------------------------
    private String determineFileType(String originalFilename) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "unknown";
        }
        int lastDot = originalFilename.lastIndexOf('.');
        return (lastDot > 0) ? originalFilename.substring(lastDot + 1) : "";
    }
}