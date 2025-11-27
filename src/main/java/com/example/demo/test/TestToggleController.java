package com.example.demo.test;

import com.example.demo.domain.service.StorageModeManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
// @Profile("dev") 등을 사용하여 개발 환경에서만 활성화할 수도 있습니다.
@RequestMapping("/api/test/storage")
public class TestToggleController {

    private final StorageModeManager storageModeManager;

    /**
     * 현재 저장소 모드를 확인합니다.
     */
    @GetMapping("/mode")
    public ResponseEntity<String> getMode() {
        String mode = storageModeManager.isLocalMode() ? "LOCAL" : "S3";
        return ResponseEntity.ok(mode);
    }

    /**
     * 저장소 모드를 전환하고 결과를 반환합니다.
     */
    @GetMapping("/toggle")
    public ResponseEntity<String> toggleMode() {
        boolean isLocal = storageModeManager.toggleMode();
        String newMode = isLocal ? "LOCAL (로컬 파일 I/O)" : "S3 (AWS 연동)";

        return ResponseEntity.ok(newMode + " Mode Activated");
    }
}