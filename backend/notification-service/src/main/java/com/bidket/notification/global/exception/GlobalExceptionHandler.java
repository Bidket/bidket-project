package com.bidket.notification.global.exception;

import com.bidket.notification.domain.exception.NotificationErrorCode;
import com.bidket.notification.domain.exception.NotificationException;
import com.bidket.notification.presentation.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 핸들러
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * NotificationException 처리
     */
    @ExceptionHandler(NotificationException.class)
    public ResponseEntity<ErrorResponse> handleNotificationException(NotificationException e) {
        log.warn("NotificationException 발생: {} - {}", e.getErrorCode().getErrorCode(), e.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode(e.getErrorCode().getErrorCode())
                .message(e.getMessage())
                .status(e.getErrorCode().getStatus().value())
                .data(null)
                .build();

        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(errorResponse);
    }

    /**
     * 기타 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("예상치 못한 예외 발생: {}", e.getMessage(), e);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode(NotificationErrorCode.INTERNAL_SERVER_ERROR.getErrorCode())
                .message(NotificationErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                .status(NotificationErrorCode.INTERNAL_SERVER_ERROR.getStatus().value())
                .data(null)
                .build();

        return ResponseEntity
                .status(NotificationErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(errorResponse);
    }
}

