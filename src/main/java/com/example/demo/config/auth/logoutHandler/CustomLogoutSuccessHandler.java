package com.example.demo.config.auth.logoutHandler;

import com.example.demo.config.auth.PrincipalDetails;
import com.example.demo.config.auth.jwt.JWTProperties;
import com.example.demo.config.auth.redis.RedisUtil;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;


@Slf4j
@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String KAKAO_CLIENT_ID;

    @Value("${spring.security.oauth2.client.registration.kakao.logout.redirect.uri}")
    private String KAKAO_REDIRECT_URI;

    @Autowired
    private RedisUtil redisUtil;


    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        log.info("CustomLogoutSuccessHandler's onLogoutSuccess invoke..! " + authentication);

        String userEmail = null;
        Cookie[] cookies = request.getCookies();

        // 1. JWT Access Token 및 Email 쿠키에서 정보 찾기
        if(cookies!=null)
        {
            String token = Arrays.stream(cookies)
                    .filter(cookie -> cookie.getName().equals(JWTProperties.ACCESS_TOKEN_COOKIE_NAME))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);

            if (token != null) {
                System.out.println("TOKEN : " + token);
            }

            userEmail = Arrays.stream(cookies)
                    .filter(cookie -> cookie.getName().equals("email"))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }


        // 2. 클라이언트 측 쿠키 무효화/삭제
        // (1) Access Token 삭제
        Cookie accessCookie = new Cookie(JWTProperties.ACCESS_TOKEN_COOKIE_NAME, null);
        accessCookie.setMaxAge(0);
        accessCookie.setPath("/");
        response.addCookie(accessCookie);

        // (2) [추가됨] Refresh Token 삭제 (반드시 추가해야 하는 부분!)
        Cookie refreshCookie = new Cookie(JWTProperties.REFRESH_TOKEN_COOKIE_NAME, null);
        refreshCookie.setMaxAge(0);
        refreshCookie.setPath("/");
        response.addCookie(refreshCookie);

        // (3) Username 삭제
        Cookie usernameCookie = new Cookie("username", null);
        usernameCookie.setMaxAge(0);
        usernameCookie.setPath("/");
        response.addCookie(usernameCookie);

        // (4) Email 삭제
        Cookie emailCookie = new Cookie("email", null);
        emailCookie.setMaxAge(0);
        emailCookie.setPath("/");
        response.addCookie(emailCookie);


        // 3. Redis Refresh Token 제거
        if(userEmail != null){
            String redisKey = "RT:" + userEmail;
            log.info("Redis RT 삭제 시도 키: {}", redisKey);
            redisUtil.delete(redisKey);
        }


        // 4. OAUTH2 외부 인증 서버 로그아웃 확인
        if(authentication != null){
            PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
            String provider = principalDetails.getDto().getProvider();
            System.out.println("provider : " + provider);

            if (provider!=null && provider.startsWith("Kakao")) {
                response.sendRedirect("https://kauth.kakao.com/oauth/logout?client_id=" + KAKAO_CLIENT_ID + "&logout_redirect_uri=" + KAKAO_REDIRECT_URI);
                return;
            } else if (provider!=null && provider.startsWith("Naver")) {
                response.sendRedirect("https://nid.naver.com/nidlogin.logout?returl=https://www.naver.com/");
                return ;
            } else if (provider!=null && provider.startsWith("Google")) {
                response.sendRedirect("https://accounts.google.com/Logout");
                return ;
            }
        }

        // 5. 최종 리다이렉트
        response.sendRedirect("/");
    }
}