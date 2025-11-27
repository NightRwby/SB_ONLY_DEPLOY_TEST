package com.example.demo.config.auth.loginHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class CustomFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        log.error("CustomFailureHandler's onAuthenticationFailure invoke...!");

        // 1. 로그인 실패 메시지 정의
        String errorMessage = "아이디 또는 비밀번호가 틀렸습니다.";

        // 2. 사용자가 입력한 'email' 값 가져오기
        // Spring Security 설정에서 usernameParameter가 "email"로 되어 있으므로 "email"로 가져옵니다.
        String email = request.getParameter("email");

        // 3. 메시지와 이메일을 인코딩
        String encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8.toString());
        String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8.toString());

        // 4. 리다이렉트 URL에 error와 email 파라미터를 모두 추가
        response.sendRedirect("/login?error=" + encodedMessage + "&email=" + encodedEmail);
    }
}