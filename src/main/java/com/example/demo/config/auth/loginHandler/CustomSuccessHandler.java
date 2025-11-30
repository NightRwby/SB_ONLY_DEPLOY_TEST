package com.example.demo.config.auth.loginHandler;

import com.example.demo.config.auth.jwt.JWTProperties;
import com.example.demo.config.auth.jwt.JWTTokenProvider;
import com.example.demo.config.auth.jwt.TokenInfo;
import com.example.demo.config.auth.redis.RedisUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    JWTTokenProvider jwtTokenProvider;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

        // ---------------------------------------------------------
        // 1. Access Token (API 요청용)
        // ---------------------------------------------------------
        // - HttpOnly: false (자바스크립트가 API 헤더에 넣어야 함)
        // - MaxAge: Refresh Token과 동일하게 설정 (만료되어도 쿠키 껍데기는 유지해서 재발급 요청 가능하게 함)
        Cookie accessCookie = new Cookie(JWTProperties.ACCESS_TOKEN_COOKIE_NAME, tokenInfo.getAccessToken());
        accessCookie.setMaxAge(JWTProperties.REFRESH_TOKEN_EXPIRATION_TIME / 1000);
        accessCookie.setPath("/");
        accessCookie.setHttpOnly(false);
        response.addCookie(accessCookie);

        // ---------------------------------------------------------
        // 2. Refresh Token (재발급 인증용 - 필수!)
        // ---------------------------------------------------------
        // - HttpOnly: true (보안상 자바스크립트는 접근 불가, 서버 전송용)
        Cookie refreshCookie = new Cookie(JWTProperties.REFRESH_TOKEN_COOKIE_NAME, tokenInfo.getRefreshToken());
        refreshCookie.setMaxAge(JWTProperties.REFRESH_TOKEN_EXPIRATION_TIME / 1000);
        refreshCookie.setPath("/");
        refreshCookie.setHttpOnly(true);
        response.addCookie(refreshCookie);

        // ---------------------------------------------------------
        // 3. [복구됨] Email 쿠키 (UI 표시용)
        // ---------------------------------------------------------
        Cookie emailCookie = new Cookie("email", authentication.getName());
        emailCookie.setMaxAge(JWTProperties.REFRESH_TOKEN_EXPIRATION_TIME / 1000);
        emailCookie.setPath("/");
        emailCookie.setHttpOnly(false); // JS에서 읽어서 화면에 보여줘야 하므로 false
        response.addCookie(emailCookie);

        // ---------------------------------------------------------
        // 4. Redis 저장 및 리다이렉트
        // ---------------------------------------------------------
        redisUtil.setDataExpire("RT:" + authentication.getName(), tokenInfo.getRefreshToken(), JWTProperties.REFRESH_TOKEN_EXPIRATION_TIME);

        String redirectUrl = "/";
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if (role.contains("ROLE_ADMIN")) { redirectUrl = "/admin"; break; }
            else if (role.contains("ROLE_MANAGER")) { redirectUrl = "/manager"; break; }
            else { redirectUrl = "/main"; break; }
        }
        response.sendRedirect(redirectUrl);
    }
}