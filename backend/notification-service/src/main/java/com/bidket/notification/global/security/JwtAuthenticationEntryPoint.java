package com.bidket.notification.global.security;

import com.bidket.notification.domain.exception.NotificationErrorCode;
import com.bidket.notification.presentation.dto.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증 실패 시 커스텀 응답을 반환하는 EntryPoint
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        
        log.warn("인증 실패: {} {} - {}", 
                request.getMethod(),
                request.getRequestURI(), 
                authException.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode(NotificationErrorCode.UNAUTHORIZED.getErrorCode())
                .message(NotificationErrorCode.UNAUTHORIZED.getMessage())
                .status(NotificationErrorCode.UNAUTHORIZED.getStatus().value())
                .data(null)
                .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}

