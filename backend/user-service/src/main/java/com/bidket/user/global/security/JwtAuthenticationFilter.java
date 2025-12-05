package com.bidket.user.global.security;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.presentation.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * JWT 인증 필터
 * 
 * Authorization 헤더에서 Bearer 토큰을 추출하여 검증하고,
 * SecurityContext에 인증 정보를 설정
 * 토큰이 있지만 유효하지 않은 경우 직접 에러 응답을 반환합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String token = extractToken(request);
        
        // 토큰이 없는 경우는 그냥 넘어가서 AuthenticationEntryPoint가 처리하도록 함
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 토큰이 있지만 유효하지 않은 경우 직접 에러 응답 반환
        Exception validationException = jwtTokenProvider.validateTokenWithException(token);
        if (validationException != null) {
            handleInvalidToken(response, validationException);
            return;
        }
        
        try {
            // 토큰이 유효한 경우 사용자 ID 추출 및 인증 설정
            UUID userId = jwtTokenProvider.getUserIdFromToken(token);
            
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    null // 권한은 추후 구현 시 추가
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            // 토큰 파싱 중 예외 발생 시
            log.warn("토큰 파싱 실패: {}", e.getMessage());
            handleInvalidToken(response, e);
            return;
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * 유효하지 않은 토큰에 대한 에러 응답 처리
     */
    private void handleInvalidToken(HttpServletResponse response, Exception exception) throws IOException {
        UserErrorCode errorCode;
        
        if (exception instanceof ExpiredJwtException) {
            errorCode = UserErrorCode.TOKEN_EXPIRED;
            log.warn("만료된 토큰: {}", exception.getMessage());
        } else {
            errorCode = UserErrorCode.INVALID_TOKEN;
            log.warn("유효하지 않은 토큰: {}", exception.getMessage());
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode(errorCode.getErrorCode())
                .message(errorCode.getMessage())
                .status(errorCode.getStatus().value())
                .data(null)
                .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * 요청 헤더에서 JWT 토큰 추출
     * 
     * @param request HTTP 요청
     * @return 추출된 토큰 (없으면 null)
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }
}

