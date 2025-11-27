package com.example.demo.config;

import com.example.demo.config.auth.exceptionHandler.CustomAccessDeniedHandler;
import com.example.demo.config.auth.exceptionHandler.CustomAuthenticationEntryPoint;
import com.example.demo.config.auth.jwt.JWTAuthorizationFilter;
import com.example.demo.config.auth.loginHandler.CustomFailureHandler;
import com.example.demo.config.auth.loginHandler.CustomSuccessHandler;
import com.example.demo.config.auth.loginHandler.OAuth2LoginSuccessHandler;
import com.example.demo.config.auth.logoutHandler.CustomLogoutHandler;
import com.example.demo.config.auth.logoutHandler.CustomLogoutSuccessHandler;
import com.example.demo.config.auth.oauth2.CookieOAuth2AuthorizationRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.HiddenHttpMethodFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig  {
    @Autowired
    CustomLogoutSuccessHandler customLogoutSuccessHandler;
    @Autowired
    CustomAccessDeniedHandler customAccessDeniedHandler;
    @Autowired
    CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    @Autowired
    CustomFailureHandler customFailureHandler;
    @Autowired
    CustomSuccessHandler customSuccessHandler;
    @Autowired
    CustomLogoutHandler customLogoutHandler;
    @Autowired
    JWTAuthorizationFilter jwtAuthorizationFilter;

    @Autowired
    OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Autowired
    CookieOAuth2AuthorizationRequestRepository cookieOAuth2AuthorizationRequestRepository;

    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {

        // csrf 비활성화(JWT 사용 시 필수)
        http.csrf((config)->{config.disable();});

        // ----------------------------------------------------
        // ✅ 1. 세션 관리 설정: STATELESS로 무상태 설정 (세션 생성 비활성화)
        // ----------------------------------------------------
        http.sessionManagement((sessionConfig)->{
            sessionConfig.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        });

        // 권한 처리
        http.authorizeHttpRequests((auth)->{
            auth.requestMatchers("/api/chat/**").authenticated();
            auth.requestMatchers(
                    "/",
                    "/main",
                    "/login",
                    "/join",
                    "/oauth2/**",
                    "/login/oauth2/code/**",
                    "/oauth2/authorization/naver",
                    "/login/oauth2/code/naver",
                    "/error",
                    "/css/**",
                    "/video/**",
                    "/js/**",
                    "/img/**",
                    "/*.html",
                    "/swagger-ui/index.html"
            ).permitAll();
            auth.requestMatchers("/user").hasAnyRole("USER");
            auth.requestMatchers("/manager").hasAnyRole("MANAGER");
            auth.requestMatchers("/admin").hasAnyRole("ADMIN");
            auth.requestMatchers("/my-account").permitAll();
            auth.anyRequest().authenticated();
        });

        // 로컬 로그인 설정
        // 폼 로그인 설정은 유지하되, 세션은 사용하지 않습니다.
        http.formLogin( (login)->{
            login.permitAll();
            login.loginPage("/login");
            login.usernameParameter("email");
            login.successHandler(customSuccessHandler);
            login.failureHandler(customFailureHandler);
        });

        // 로그아웃 설정 (JWT 사용 환경에서는 토큰 무효화 로직이 중요)
        http.logout( (logout)->{
            logout.permitAll();
            logout.addLogoutHandler(customLogoutHandler);
            logout.logoutSuccessHandler(customLogoutSuccessHandler);
        } );

        // 예외 처리
        http.exceptionHandling((ex)->{
            ex.authenticationEntryPoint(customAuthenticationEntryPoint);
            ex.accessDeniedHandler(customAccessDeniedHandler);
        });

        // Oauth2-Client 활성화
        http.oauth2Login((oauth2)->{
            oauth2.loginPage("/login");
            oauth2.successHandler(oAuth2LoginSuccessHandler);
            // OAuth2의 Authorization Request 저장을 세션이 아닌 쿠키로 대체
            oauth2.authorizationEndpoint(authEndpoint ->
                    authEndpoint.authorizationRequestRepository(cookieOAuth2AuthorizationRequestRepository)
            );
        });

        // ----------------------------------------------------
        // ✅ 2. TokenFilter 추가 (JWT가 세션을 대신하여 인증을 담당)
        // ----------------------------------------------------
        // UsernamePasswordAuthenticationFilter 이전에 JWT 필터를 추가하여 토큰 검증을 먼저 수행합니다.
        http.addFilterBefore(jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter.class );

        // Etc..
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public FilterRegistrationBean<HiddenHttpMethodFilter> hiddenHttpMethodFilter() {
        FilterRegistrationBean<HiddenHttpMethodFilter> filterRegistrationBean = new FilterRegistrationBean<>(new HiddenHttpMethodFilter());

        filterRegistrationBean.setOrder(100);
        return filterRegistrationBean;
    }
}