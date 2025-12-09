package com.bidket.user.global.security;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.presentation.dto.response.ErrorResponse;
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
 * (인증이 실패했을 때 어떤 응답을 보낼지 결정하는 시작 지점)
 * 인증이 필요한 엔드포인트에 접근할 때 토큰이 없거나 유효하지 않은 경우
 * 커스텀 에러 응답을 반환합니다.
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        
        log.warn("인증 실패: {} - {}", request.getRequestURI(), authException.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode(UserErrorCode.UNAUTHORIZED.getErrorCode())
                .message(UserErrorCode.UNAUTHORIZED.getMessage())
                .status(UserErrorCode.UNAUTHORIZED.getStatus().value())
                .data(null)
                .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}

