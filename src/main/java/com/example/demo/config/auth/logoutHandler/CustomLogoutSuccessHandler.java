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


    // 로컬서버 로그아웃 이후 추가 처리(ex. 카카오인증서버 연결해제..)
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // authentication이 null로 들어와도 로그아웃 처리가 되도록 수정합니다.
        log.info("CustomLogoutSuccessHandler's onLogoutSuccess invoke..! " + authentication);

        String userEmail = null;
        Cookie[] cookies = request.getCookies();

        // 1. JWT Access Token 및 Email 쿠키에서 정보 찾기
        if(cookies!=null)
        {
            // Access Token 쿠키 값 추출 (OAUTH2 로직에 사용됨)
            String token = Arrays.stream(cookies)
                    .filter(cookie -> cookie.getName().equals(JWTProperties.ACCESS_TOKEN_COOKIE_NAME))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);

            if (token != null) {
                System.out.println("TOKEN : " + token);
            }

            // Email 쿠키에서 userEmail 추출 (Authentication이 null일 때 Redis 삭제에 사용)
            userEmail = Arrays.stream(cookies)
                    .filter(cookie -> cookie.getName().equals("email"))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }


        // 2. 클라이언트 측 쿠키 무효화/삭제 (기존 로직 유지)
        Cookie accessCookie = new Cookie(JWTProperties.ACCESS_TOKEN_COOKIE_NAME, null);
        accessCookie.setMaxAge(0);
        accessCookie.setPath("/");
        response.addCookie(accessCookie);

        Cookie usernameCookie = new Cookie("username", null);
        usernameCookie.setMaxAge(0);
        usernameCookie.setPath("/");
        response.addCookie(usernameCookie);

        Cookie emailCookie = new Cookie("email", null);
        emailCookie.setMaxAge(0);
        emailCookie.setPath("/");
        response.addCookie(emailCookie);


        // 3. Redis Refresh Token 제거 (Authentication 유무와 무관하게 email 쿠키로 실행)
        if(userEmail != null){
            // 💡 Redis에 저장할 때 사용한 키 형식(띄어쓰기 유무)을 정확히 맞춰주세요.
            // 현재 코드에서는 띄어쓰기 없는 "RT:"를 사용합니다.
            String redisKey = "RT:" + userEmail;
            log.info("Redis RT 삭제 시도 키: {}", redisKey);
            redisUtil.delete(redisKey);
        }


        // 4. OAUTH2 외부 인증 서버 로그아웃 확인 (Authentication 객체가 있을 때만 처리, 기존 로직 유지)
        if(authentication != null){

            // OAUTH2 외부 인증 서버 로그아웃 확인
            PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
            String provider = principalDetails.getDto().getProvider();
            System.out.println("provider : " + provider);

            if (provider!=null && provider.startsWith("Kakao")) {
                System.out.println("!!!" + KAKAO_CLIENT_ID + " " + KAKAO_REDIRECT_URI);
                response.sendRedirect("https://kauth.kakao.com/oauth/logout?client_id=" + KAKAO_CLIENT_ID + "&logout_redirect_uri=" + KAKAO_REDIRECT_URI);
                return; // 리다이렉트 후 종료
            } else if (provider!=null && provider.startsWith("Naver")) {
                response.sendRedirect("https://nid.naver.com/nidlogin.logout?returl=https://www.naver.com/");
                return ; // 리다이렉트 후 종료
            } else if (provider!=null && provider.startsWith("Google")) {
                response.sendRedirect("https://accounts.google.com/Logout");
                return ; // 리다이렉트 후 종료
            }
        }

        // 5. 최종 리다이렉트 (OAUTH 리다이렉트가 일어나지 않은 경우)
        response.sendRedirect("/");
    }
}