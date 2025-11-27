package com.example.demo.config.auth.jwt;

import com.example.demo.config.auth.redis.RedisUtil;
import com.example.demo.domain.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JWTAuthorizationFilter extends OncePerRequestFilter {

    @Autowired
    JWTTokenProvider jwtTokenProvider;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RedisUtil redisUtil;

    // JWTAuthorizationFilter는 요청 시 단 한 번 실행됩니다.
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        // 특정 DELETE 요청에 대한 필터 제외 (기존 로직 유지)
        if (request.getMethod().equals("DELETE") && request.getRequestURI().equals("/my-account")) {
            filterChain.doFilter(request, response);
            return;
        }

        System.out.println("[JWTAuthorizationFilter] doFilterInternal ...");

        String accessToken = null;
        String clientRefreshToken = null;

        // 1. Authorization Header에서 Access Token 추출
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            accessToken = header.replace("Bearer ", "");
            System.out.println("TOKEN (from Header): " + accessToken);
        }

        // 2. Cookie에서 Access Token 및 Refresh Token 추출
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            if (accessToken == null) {
                // AccessToken이 Header에 없으면 쿠키에서 추출 시도
                accessToken = Arrays.stream(cookies)
                        .filter((cookie) -> cookie.getName().equals(JWTProperties.ACCESS_TOKEN_COOKIE_NAME))
                        .findFirst()
                        .map(Cookie::getValue)
                        .orElse(null);
            }

            // RefreshToken은 항상 쿠키에서 추출
            clientRefreshToken = Arrays.stream(cookies)
                    .filter((cookie) -> cookie.getName().equals(JWTProperties.REFRESH_TOKEN_COOKIE_NAME))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }

        System.out.println("AccessToken : " + accessToken);
        System.out.println("Client RefreshToken : " + clientRefreshToken);


        // AccessToken이 존재하는 경우에만 처리 시작
        if (accessToken != null) {

            // 블랙리스트(로그아웃된 토큰) 검증
            if (redisUtil.hasKey(accessToken)) {
                log.warn("Blacklisted Token Detected: {}", accessToken);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Revoked token access denied. Please log in again.");

                // 쿠키 삭제
                response.addCookie(deleteTokenCookie(JWTProperties.ACCESS_TOKEN_COOKIE_NAME));
                response.addCookie(deleteTokenCookie(JWTProperties.REFRESH_TOKEN_COOKIE_NAME));
                return;
            }

            // 1. Access Token 유효성 검증
            try {
                if (jwtTokenProvider.validateToken(accessToken)) {
                    // 1-1. AccessToken이 유효한 경우 -> SecurityContext에 인증 정보 설정
                    Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("SecurityContext 인증 완료: {}", SecurityContextHolder.getContext().getAuthentication().getName());
                }
            } catch (ExpiredJwtException e) {
                // 1-2. AccessToken이 만료된 경우 -> Refresh Token을 이용한 재발급 시도
                log.info("ExpiredJwtException... AccessToken Expired: {}", e.getMessage());

                String expiredEmail = null;
                try {
                    // 만료된 AT에서 이메일을 안전하게 추출 (getExpiredEmail 메서드 사용)
                    expiredEmail = jwtTokenProvider.getExpiredEmail(accessToken);
                    log.info("Extracted email from expired token: {}", expiredEmail);
                } catch (JwtException ex) {
                    // 토큰 서명 자체가 유효하지 않으면 이메일 추출 불가 -> 재발급 불가
                    log.error("ERROR: Failed to extract email from expired token for RT check. Token corrupted.");
                    // SecurityContextHolder는 비우고, 다음 필터로 진행
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                // 2. Refresh Token 기반 재발급 로직
                if (clientRefreshToken != null && expiredEmail != null) {

                    // 3. Redis에 저장된 Refresh Token 조회 (키: "RT:expiredEmail")
                    String redisRefreshToken = redisUtil.getStringData("RT:" + expiredEmail);

                    // 4. 클라이언트의 RT와 Redis의 RT가 일치하는지 확인
                    if (redisRefreshToken != null && clientRefreshToken.equals(redisRefreshToken)) {
                        try {
                            // 5. Refresh Token 자체의 유효성 검증
                            if (jwtTokenProvider.validateToken(clientRefreshToken)) {
                                // Refresh Token 유효함 -> AccessToken 재발급 로직 실행

                                // RT에서 권한 정보 가져오기 (RT에도 권한 정보가 있다고 가정)
                                Authentication authFromRT = jwtTokenProvider.getAuthentication(clientRefreshToken);
                                String userAuth = authFromRT.getAuthorities().stream()
                                        .map(GrantedAuthority::getAuthority)
                                        .collect(Collectors.joining(","));

                                // 6. 새로운 Access/Refresh Token 쌍 발급 (reissueTokens 메서드 사용)
                                TokenInfo newTokenInfo = jwtTokenProvider.reissueTokens(expiredEmail, userAuth);

                                // 7. Redis에 새로운 Refresh Token으로 갱신
                                long rtExpirationSec = JWTProperties.REFRESH_TOKEN_EXPIRATION_TIME / 1000;
                                redisUtil.setDataExpire(
                                        "RT:" + expiredEmail,
                                        newTokenInfo.getRefreshToken(),
                                        rtExpirationSec
                                );

                                // 8. 새로운 Access Token 쿠키 추가
                                response.addCookie(createTokenCookie(JWTProperties.ACCESS_TOKEN_COOKIE_NAME,
                                        newTokenInfo.getAccessToken(),
                                        JWTProperties.ACCESS_TOKEN_EXPIRATION_TIME / 1000));

                                // 9. 새로운 Refresh Token 쿠키 추가 (기존 것을 갱신)
                                response.addCookie(createTokenCookie(JWTProperties.REFRESH_TOKEN_COOKIE_NAME,
                                        newTokenInfo.getRefreshToken(),
                                        rtExpirationSec));

                                // 10. SecurityContext에 인증 정보 설정 후 다음 필터로 진행
                                // 새로 발급된 Access Token으로 Authentication 객체 생성하여 설정
                                Authentication newAuthentication = jwtTokenProvider.getAuthentication(newTokenInfo.getAccessToken());
                                SecurityContextHolder.getContext().setAuthentication(newAuthentication);
                                log.info("토큰 재발급 성공 및 SecurityContext 인증 완료: {}", expiredEmail);

                            }
                        } catch (ExpiredJwtException e2) {
                            // Refresh Token도 만료된 경우 (재로그인 필요)
                            log.warn("Refresh Token 만료. 사용자: {}", expiredEmail);
                            handleTokenExpiration(response, expiredEmail);
                        } catch (Exception e3) {
                            log.error("토큰 재발급 중 알 수 없는 오류 발생: {}", e3.getMessage());
                            handleTokenExpiration(response, expiredEmail);
                        }
                    } else {
                        // Redis의 토큰과 불일치하거나 Redis에 토큰이 없는 경우 (토큰 변조 의심 또는 강제 로그아웃)
                        log.warn("Redis Refresh Token 불일치 또는 토큰이 유효하지 않음. 사용자: {}", expiredEmail);
                        handleTokenExpiration(response, expiredEmail);
                    }
                } else {
                    // Refresh Token이 아예 없거나 이메일 추출에 실패한 경우
                    log.warn("Refresh Token 없음 또는 이메일 추출 실패. 재로그인 필요.");
                    handleTokenExpiration(response, expiredEmail);
                }

            } catch (Exception otherException) {
                // AT 유효성 검증 실패 (만료 외의 이유: 서명 오류, 형식 오류 등)
                log.error("AccessToken 유효성 검증 실패 (만료 외): {}", otherException.getMessage());
                // SecurityContextHolder는 비우고, 다음 필터로 진행
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }


    // =========================================================
    // 헬퍼 메서드
    // =========================================================

    /**
     * 토큰 만료 처리 (쿠키 삭제 및 Redis에서 RT 삭제)
     */
    private void handleTokenExpiration(HttpServletResponse response, String expiredEmail) {
        // Access/Refresh Token 쿠키 삭제
        response.addCookie(deleteTokenCookie(JWTProperties.ACCESS_TOKEN_COOKIE_NAME));
        response.addCookie(deleteTokenCookie(JWTProperties.REFRESH_TOKEN_COOKIE_NAME));

        // Redis에서 Refresh Token 삭제
        if (expiredEmail != null) {
            redisUtil.delete("RT:" + expiredEmail);
        }
        // 응답 상태 설정 (필요 시: 클라이언트에게 재로그인 필요 알림)
        // response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * 토큰 쿠키 생성
     */
    private Cookie createTokenCookie(String name, String value, long maxAge) { // maxAge 타입을 long으로 변경 (JWTProperties와 일치하도록)
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge((int) maxAge);
        cookie.setPath("/");
        cookie.setHttpOnly(true); // XSS 방지
        cookie.setSecure(true);   // HTTPS에서만 전송 (운영 환경 권장)
        return cookie;
    }

    /**
     * 토큰 쿠키 삭제 (만료 시간 0)
     */
    private Cookie deleteTokenCookie(String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true); // 일관성 유지
        cookie.setSecure(true);   // 일관성 유지
        return cookie;
    }
}