package com.example.demo.config.auth.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class StompHandler implements ChannelInterceptor {

    private final JWTTokenProvider jwtTokenProvider;

    // 메시지가 채널로 전송되기 전에 실행됨
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // WebSocket 연결 시(CONNECT 프레임일 때) 헤더 검증
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("[StompHandler] WebSocket 연결 요청 감지");

            // 1. 헤더에서 토큰 추출 (Authorization 또는 access-token)
            String jwtToken = accessor.getFirstNativeHeader("Authorization");
            if (jwtToken == null) {
                jwtToken = accessor.getFirstNativeHeader("access-token");
            }

            // 2. Bearer 제거
            if (jwtToken != null && jwtToken.startsWith("Bearer ")) {
                jwtToken = jwtToken.substring(7);
            }

            // 3. 토큰 유효성 검증 및 인증 객체 설정
            try {
                if (jwtToken != null && jwtTokenProvider.validateToken(jwtToken)) {
                    Authentication auth = jwtTokenProvider.getAuthentication(jwtToken);
                    accessor.setUser(auth); // 핵심: WebSocket 세션에 인증 정보 주입
                    log.info("[StompHandler] 인증 성공: {}", auth.getName());
                } else {
                    log.warn("[StompHandler] 유효하지 않은 토큰입니다.");
                    // 필요 시 여기서 예외를 던져 연결을 끊을 수 있음
                }
            } catch (Exception e) {
                log.error("[StompHandler] 토큰 검증 중 오류: {}", e.getMessage());
            }
        }
        return message;
    }
}