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
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;


import java.io.IOException;

@Slf4j
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    JWTTokenProvider jwtTokenProvider;

    @Autowired
    RedisUtil redisUtil;

    // Refresh Token의 만료 시간(밀리초)을 초(MaxAge) 단위로 변환
    private final int refreshTokenMaxAge = (int) (JWTProperties.REFRESH_TOKEN_EXPIRATION_TIME / 1000);
    private final int accessTokenMaxAge = (int) (JWTProperties.ACCESS_TOKEN_EXPIRATION_TIME / 1000);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        log.info("[OAuth2LoginSuccessHandler] Social Login Success! Generating JWT tokens...");

        // 1. JWT 토큰 생성 (AT, RT)
        TokenInfo tokenInfo = jwtTokenProvider.generateToken(authentication);

        // 2. Access Token을 쿠키로 전달
        addCookie(response, JWTProperties.ACCESS_TOKEN_COOKIE_NAME, tokenInfo.getAccessToken(), accessTokenMaxAge);

        // 3. Refresh Token을 쿠키로 전달+ (JWTAuthorizationFilter에서 재발급 시 사용됨)
        addCookie(response, JWTProperties.REFRESH_TOKEN_COOKIE_NAME, tokenInfo.getRefreshToken(), refreshTokenMaxAge);

        // 4. REIDS에 Refresh Token 저장
        // Key: "RT:username", Value: Refresh Token, Expire Time: Refresh Token의 만료 시간
        redisUtil.setDataExpire("RT:"+authentication.getName(),
                tokenInfo.getRefreshToken(),
                JWTProperties.REFRESH_TOKEN_EXPIRATION_TIME); // RT 만료 시간(밀리초) 사용

        // 5. username 쿠키 전달 (사용자 식별용)
        addCookie(response, "username", authentication.getName(), refreshTokenMaxAge);


        log.info("OAuth2LoginSuccessHandler's onAuthenticationSuccess invoke...genToken.."+tokenInfo);

        // 6. 리다이렉션 (메인 페이지나 토큰 처리 페이지로 이동)
        String redirectUrl = "/user";
        response.sendRedirect(redirectUrl);

    }

    //쿠키를 생성하여 응답에 추가하는 헬퍼 메서드
    private void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(maxAge); // Refresh Token의 만료 시간과 동일하게 설정
        cookie.setPath("/");
        cookie.setHttpOnly(true); // XSS 방지를 위해 필수.
        // cookie.setSecure(true);
        response.addCookie(cookie);
    }
}
