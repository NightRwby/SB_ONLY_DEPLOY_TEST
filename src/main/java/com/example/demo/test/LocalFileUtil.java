package com.example.demo.test;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

// 이 클래스는 test 패키지의 존재 여부만으로 Local Mode를 활성화시키는 핵심 파일입니다.
public class LocalFileUtil {

    // 🔑 로컬 파일이 실제로 저장될 기본 경로
    // (개발자 PC의 임시 경로로 설정)
    private static final Path FILE_STORAGE_LOCATION =
            Paths.get("C:/temp/uploads/chat-files").toAbsolutePath().normalize();

    /**
     * 로컬 파일 시스템에 파일을 저장합니다.
     * @param file 클라이언트 파일
     * @param dirPath S3 키와 유사하게 사용할 경로 (예: chat/room-1)
     * @return 로컬 파일 경로(Storage Key로 사용)
     */
    public static String saveToLocal(MultipartFile file, String dirPath) {
        // 1. 저장 디렉토리 생성
        try{
            Path targetDirectory = FILE_STORAGE_LOCATION.resolve(dirPath);
            if (Files.notExists(targetDirectory)) {
                Files.createDirectories(targetDirectory);
            }

            // 2. 고유 파일명 생성
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.contains(".") ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String storageKey = dirPath + "/" + UUID.randomUUID().toString() + extension;

            Path targetPath = FILE_STORAGE_LOCATION.resolve(storageKey).normalize();

            // 3. 파일 저장
            Files.copy(file.getInputStream(), targetPath);

            return storageKey; // S3 키처럼 사용될 로컬 경로 반환
        } catch (IOException e) {
            System.err.println("로컬 파일 저장 실패: " + e.getMessage());
            return null;
        }
    }

    /**
     * 로컬 파일 시스템에서 파일을 읽어 Resource로 반환합니다.
     */
    public static Resource loadFromLocal(String storageKey) {
        try {
            Path targetPath = FILE_STORAGE_LOCATION.resolve(storageKey).normalize();

            System.out.println("DEBUG_LOAD: Trying to load file from:" + targetPath.toAbsolutePath());

            // 경로 탐색 공격 방지
            if (!targetPath.startsWith(FILE_STORAGE_LOCATION)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 파일 경로 요청입니다.");
            }

            Resource resource = new UrlResource(targetPath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                String errorMessage = String.format("로컬에 파일이 없습니다 (Exists: %s, Readable: %s): %s",
                        resource.exists(), resource.isReadable(), storageKey);
                System.err.println("DEBUG_LOAD_FAIL: " + errorMessage);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "로컬에 파일이 없습니다: " + storageKey);

            }
        } catch (MalformedURLException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일 경로가 잘못되었습니다.", ex);
        } catch (Exception ex) {
            // 기타 I/O 오류 처리
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "로컬 파일 로드 중 오류 발생", ex);
        }
    }

    //프로필 이미지 삭제 메서드
    public static void deleteFromLocal(String storageKey) {
        try {
            Path targetPath = FILE_STORAGE_LOCATION.resolve(storageKey).normalize();

            // 경로 탐색 공격 방지
            if (!targetPath.startsWith(FILE_STORAGE_LOCATION)) {
                // 잘못된 경로이므로 삭제 시도 안 함
                return;
            }

            // 파일이 존재하면 삭제
            if (Files.exists(targetPath)) {
                Files.delete(targetPath);
                System.out.println("로컬 파일 삭제 성공: " + storageKey);
            } else {
                System.out.println("삭제할 파일이 로컬에 없습니다: " + storageKey);
            }
        } catch (IOException ex) {
            // I/O 오류 발생 시 예외 처리
            System.err.println("로컬 파일 삭제 중 오류 발생: " + storageKey + " - " + ex.getMessage());
            // 필요하다면 RuntimeException으로 다시 던지거나 로그 기록
        }
    }

}