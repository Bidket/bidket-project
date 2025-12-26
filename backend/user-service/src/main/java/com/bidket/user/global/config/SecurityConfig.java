package com.bidket.user.global.config;

import com.bidket.user.global.security.JwtAuthenticationEntryPoint;
import com.bidket.user.global.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;

/**
 * Spring Security 설정
 * 회원가입, 로그인 엔드포인트는 인증 없이 접근 가능하도록 설정합니다.
 * 내 정보 조회 등 인증이 필요한 엔드포인트는 JWT 토큰 검증이 필요합니다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final Environment environment;

    /**
     * Security 필터 체인 설정
     * - CSRF 비활성화
     * - Stateless 세션 정책 (JWT 사용)
     * - JWT 인증 필터 추가
     * - 인증 실패 시 커스텀 EntryPoint 사용
     * - /v1/members/signup, /v1/members/login, /v1/members/social-login, /v1/members/token/refresh, /v1/members/check-email, /v1/members/check-nickname 엔드포인트는 인증 없이 접근 가능
     * - /v1/members/me 등 그 외 엔드포인트는 인증 필요
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .authorizeHttpRequests(auth -> {
                    // 공개 API
                    auth.requestMatchers("/v1/members/signup", "/v1/members/login", "/v1/members/social-login", 
                            "/v1/members/token/refresh", "/v1/members/check-email", "/v1/members/check-nickname", 
                            "/v1/members/google-client-id").permitAll();
                    
                    // 내부 서비스 간 통신용 API - 인증 필요 (보안 강화)
                    // TODO: 추후 서비스 간 전용 토큰 또는 헤더 기반 인증 추가
                    auth.requestMatchers("/api/v1/users/**").authenticated();
                    
                    // Swagger UI
                    auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll();
                    
                    // 테스트용 HTML 파일 - dev 또는 local 프로필에서만 허용 (운영 노출 방지)
                    String[] activeProfiles = environment.getActiveProfiles();
                    boolean isDevOrLocalProfile = Arrays.asList(activeProfiles).contains("dev") || 
                                                   Arrays.asList(activeProfiles).contains("local") ||
                                                   activeProfiles.length == 0; // 프로필이 없을 때도 허용 (기본 로컬 환경)
                    if (isDevOrLocalProfile) {
                        auth.requestMatchers("/*.html", "/static/**", "/google-login-test.html").permitAll();
                    }
                    
                    // 그 외 모든 요청은 인증 필요
                    auth.anyRequest().authenticated();
                });

        return http.build();
    }
}

