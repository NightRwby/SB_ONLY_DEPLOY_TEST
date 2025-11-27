package com.example.demo.config.auth.exceptionHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        log.error("CustomAuthenticationEntryPoint's commence invoke....!");
        log.error("CustomAuthenticationEntryPoint's commence invoke....! Request URI: {}", request.getRequestURI());

        // 오류 메시지 인코딩 시 예외 메시지 대신 고정된 문자열 사용
        String errorMessage = "로그인이 필요합니다.";
        String encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8.toString());

        // ROLE 별로 기본페이지로 이동
        response.sendRedirect("/login?error=" + encodedMessage);
    }
}
