package com.bidket.notification.global.security;

import com.bidket.notification.domain.exception.NotificationErrorCode;
import com.bidket.notification.presentation.dto.response.ErrorResponse;
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
        
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        
        Exception validationException = jwtTokenProvider.validateTokenWithException(token);
        if (validationException != null) {
            handleInvalidToken(response, validationException);
            return;
        }
        
        try {
            UUID userId = jwtTokenProvider.getUserIdFromToken(token);
            
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    null
            );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.warn("토큰 파싱 실패: {}", e.getMessage());
            handleInvalidToken(response, e);
            return;
        }
        
        filterChain.doFilter(request, response);
    }

    private void handleInvalidToken(HttpServletResponse response, Exception exception) throws IOException {
        NotificationErrorCode errorCode;
        
        if (exception instanceof ExpiredJwtException) {
            errorCode = NotificationErrorCode.TOKEN_EXPIRED;
            log.warn("만료된 토큰: {}", exception.getMessage());
        } else {
            errorCode = NotificationErrorCode.INVALID_TOKEN;
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

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }
}

