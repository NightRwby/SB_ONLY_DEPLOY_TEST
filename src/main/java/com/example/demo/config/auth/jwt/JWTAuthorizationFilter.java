package com.example.demo.config.auth.jwt;

import com.example.demo.config.auth.redis.RedisUtil;
import com.example.demo.domain.entity.user.User;
import com.example.demo.domain.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Component
@Slf4j
public class JWTAuthorizationFilter extends OncePerRequestFilter {

    @Autowired
    JWTTokenProvider jwtTokenProvider;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RedisUtil redisUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");

        // 특정 DELETE 요청에 대한 필터 제외
        if (request.getMethod().equals("DELETE") && request.getRequestURI().equals("/my-account")) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = null;
        String clientRefreshToken = null;

        // 1. Authorization Header 추출
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            accessToken = header.replace("Bearer ", "");
        }

        // 2. Cookie 추출
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            if (accessToken == null) {
                accessToken = Arrays.stream(cookies)
                        .filter(cookie -> cookie.getName().equals(JWTProperties.ACCESS_TOKEN_COOKIE_NAME))
                        .findFirst()
                        .map(Cookie::getValue)
                        .orElse(null);
            }
            clientRefreshToken = Arrays.stream(cookies)
                    .filter(cookie -> cookie.getName().equals(JWTProperties.REFRESH_TOKEN_COOKIE_NAME))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }

        if (accessToken != null) {
            // 블랙리스트 체크
            if (redisUtil.hasKey(accessToken)) {
                log.warn("Blacklisted Token Detected");
                handleTokenExpiration(response, null);
                return;
            }

            try {
                // Access Token 유효성 검사
                if (jwtTokenProvider.validateToken(accessToken)) {
                    Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (ExpiredJwtException e) {
                // -----------------------------------------------------------
                // 🟢 Access Token 만료 시 재발급 로직
                // -----------------------------------------------------------
                log.info("Access Token 만료. 재발급 시도 중...");

                String expiredEmail = null;
                try {
                    expiredEmail = jwtTokenProvider.getExpiredEmail(accessToken);
                } catch (Exception ex) {
                    log.error("만료된 토큰에서 이메일 추출 실패");
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                if (clientRefreshToken != null && expiredEmail != null) {
                    // Redis 검증
                    String redisRefreshToken = redisUtil.getStringData("RT:" + expiredEmail);

                    if (redisRefreshToken != null && clientRefreshToken.equals(redisRefreshToken)) {
                        try {
                            // Refresh Token 유효성 검증
                            if (jwtTokenProvider.validateToken(clientRefreshToken)) {

                                // DB에서 사용자 정보 조회
                                Optional<User> userOptional = userRepository.findByEmail(expiredEmail);

                                if (userOptional.isPresent()) {
                                    User user = userOptional.get();
                                    String userAuth = user.getRole();

                                    // 토큰 재발급
                                    TokenInfo newTokenInfo = jwtTokenProvider.reissueTokens(expiredEmail, userAuth);

                                    // Redis 갱신
                                    redisUtil.setDataExpire(
                                            "RT:" + expiredEmail,
                                            newTokenInfo.getRefreshToken(),
                                            JWTProperties.REFRESH_TOKEN_EXPIRATION_TIME / 1000
                                    );

                                    // 🟢 [수정됨] 쿠키 갱신 (인자 4개 사용 & AccessToken MaxAge 증가)

                                    // 1. Access Token: HttpOnly=false (JS 허용), MaxAge=RefreshTime (유지)
                                    response.addCookie(createTokenCookie(
                                            JWTProperties.ACCESS_TOKEN_COOKIE_NAME,
                                            newTokenInfo.getAccessToken(),
                                            JWTProperties.REFRESH_TOKEN_EXPIRATION_TIME / 1000,
                                            false
                                    ));

                                    // 2. Refresh Token: HttpOnly=true (JS 차단)
                                    response.addCookie(createTokenCookie(
                                            JWTProperties.REFRESH_TOKEN_COOKIE_NAME,
                                            newTokenInfo.getRefreshToken(),
                                            JWTProperties.REFRESH_TOKEN_EXPIRATION_TIME / 1000,
                                            true
                                    ));

                                    // SecurityContext 인증 정보 갱신
                                    Authentication newAuthentication = jwtTokenProvider.getAuthentication(newTokenInfo.getAccessToken());
                                    SecurityContextHolder.getContext().setAuthentication(newAuthentication);

                                    log.info("토큰 재발급 성공: {}", expiredEmail);
                                } else {
                                    log.error("사용자 정보를 찾을 수 없음: {}", expiredEmail);
                                    handleTokenExpiration(response, expiredEmail);
                                }
                            }
                        } catch (Exception re) {
                            log.error("Refresh Token 검증 실패 또는 재발급 오류: {}", re.getMessage());
                            handleTokenExpiration(response, expiredEmail);
                        }
                    } else {
                        log.warn("Redis Refresh Token 불일치");
                        handleTokenExpiration(response, expiredEmail);
                    }
                } else {
                    log.warn("Refresh Token 없음");
                    handleTokenExpiration(response, expiredEmail);
                }
            } catch (Exception e) {
                log.error("Access Token 오류 (서명 등): {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private void handleTokenExpiration(HttpServletResponse response, String email) {
        response.addCookie(deleteTokenCookie(JWTProperties.ACCESS_TOKEN_COOKIE_NAME));
        response.addCookie(deleteTokenCookie(JWTProperties.REFRESH_TOKEN_COOKIE_NAME));
        if (email != null) {
            redisUtil.delete("RT:" + email);
        }
    }

    private Cookie createTokenCookie(String name, String value, long maxAge, boolean httpOnly) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge((int) maxAge);
        cookie.setPath("/");
        cookie.setHttpOnly(httpOnly);
        return cookie;
    }

    private Cookie deleteTokenCookie(String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        return cookie;
    }
}