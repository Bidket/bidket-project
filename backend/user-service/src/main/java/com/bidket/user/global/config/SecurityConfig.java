package com.bidket.user.global.config;

import com.bidket.user.global.security.JwtAuthenticationEntryPoint;
import com.bidket.user.global.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정
 * 
 * 회원가입, 로그인 엔드포인트는 인증 없이 접근 가능하도록 설정합니다.
 * 내 정보 조회 등 인증이 필요한 엔드포인트는 JWT 토큰 검증이 필요합니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

 * 회원가입 엔드포인트는 인증 없이 접근 가능하도록 설정합니다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Security 필터 체인 설정
     *
     * - CSRF 비활성화
     * - Stateless 세션 정책 (JWT 사용)
     * - JWT 인증 필터 추가
     * - 인증 실패 시 커스텀 EntryPoint 사용
     * - /v1/members/signup, /v1/members/login, /v1/members/check-email 엔드포인트는 인증 없이 접근 가능
     * - /v1/members/me 등 그 외 엔드포인트는 인증 필요
     * - /v1/members/signup, /v1/members/login 엔드포인트는 인증 없이 접근 가능
     * - /v1/members/me 등 그 외 엔드포인트는 인증 필요
     * - /v1/members/signup 엔드포인트는 인증 없이 접근 가능
     * - /v1/members/signup, /v1/members/login 엔드포인트는 인증 없이 접근 가능
     * - 그 외 엔드포인트는 인증 필요
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
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v1/members/signup", "/v1/members/login", "/v1/members/check-email").permitAll()
                        .anyRequest().authenticated()  // 인증이 필요한 엔드포인트
                        .requestMatchers("/v1/members/signup", "/v1/members/login").permitAll()
                        .anyRequest().authenticated()  // 인증이 필요한 엔드포인트
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v1/members/signup", "/v1/members/login").permitAll()
                        .anyRequest().permitAll()  // TODO: 개발 단계 - 추후 인증 필요 엔드포인트로 변경
                );

        return http.build();
    }
}

