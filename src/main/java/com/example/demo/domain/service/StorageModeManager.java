package com.example.demo.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;

@Component
@Slf4j
public class StorageModeManager {

    // 💡 테스트 코드가 존재하는지 확인할 클래스의 정규화된 이름
    private static final String TEST_CLASS_NAME = "com.example.demo.test.LocalFileUtil";

    // 🔑 로컬 모드 상태를 보유하는 변수
    private boolean isLocalMode;

    // 🔑 개발 환경에서 수동으로 모드를 강제하기 위한 플래그 (선택적)
    @Value("${storage.mode.force-s3:false}")
    private boolean forceS3Mode;

    /**
     * 애플리케이션 시작 시점에 초기화됩니다.
     */
    @PostConstruct
    public void init() {
        if (forceS3Mode) {
            this.isLocalMode = false;
            log.warn("STORAGE MODE: S3 Mode is FORCE-ENABLED by configuration.");
            return;
        }

        try {
            // 1. 클래스로더를 사용하여 특정 테스트 클래스 (LocalFileUtil)를 찾습니다.
            Class.forName(TEST_CLASS_NAME);

            // 2. 클래스 로드에 성공하면 (테스트 파일 존재), 로컬 모드 활성화
            this.isLocalMode = true;
            log.warn("STORAGE MODE: LOCAL MODE activated. Detected test class [{}]", TEST_CLASS_NAME);

        } catch (ClassNotFoundException e) {
            // 3. 클래스 로드에 실패하면 (테스트 파일 삭제), S3 모드 활성화
            this.isLocalMode = false;
            log.info("STORAGE MODE: S3 MODE activated. Test class [{}] not found.", TEST_CLASS_NAME);
        }
    }

    /**
     * 현재 저장소 모드가 로컬인지 확인합니다.
     */
    public boolean isLocalMode() {
        return isLocalMode;
    }

    /**
     * 저장소 모드를 수동으로 전환합니다. (개발 시 버튼용)
     */
    public boolean toggleMode() {
        this.isLocalMode = !this.isLocalMode;
        log.warn("STORAGE MODE: Mode manually toggled to {}", this.isLocalMode ? "LOCAL" : "S3");
        return this.isLocalMode;
    }
}