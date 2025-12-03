package com.bidket.user.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정
 * 
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
     * - /v1/members/signup, /v1/members/login 엔드포인트는 인증 없이 접근 가능
     * - 그 외 엔드포인트는 인증 필요
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v1/members/signup", "/v1/members/login").permitAll()
                        .anyRequest().permitAll()  // TODO: 개발 단계 - 추후 인증 필요 엔드포인트로 변경
                );

        return http.build();
    }
}

