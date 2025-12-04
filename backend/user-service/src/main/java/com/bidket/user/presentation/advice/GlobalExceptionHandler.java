package com.bidket.user.presentation.advice;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.presentation.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리 핸들러
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 사용자 커스텀 예외 처리
     */
    @ExceptionHandler(UserException.class)
    public ResponseEntity<ErrorResponse> handleUserException(UserException e) {
        UserErrorCode errorCode = e.getErrorCode();
        log.warn("사용자 예외 발생: {} - {}", errorCode.getErrorCode(), errorCode.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode(errorCode.getErrorCode())
                .message(errorCode.getMessage())
                .status(errorCode.getStatus().value())
                .data(null)
                .build();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(errorResponse);
    }

    /**
     * 유효성 검증 실패 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException e) {
        // 이메일 필드 에러인지 확인
        boolean isEmailError = e.getBindingResult().getFieldErrors().stream()
                .anyMatch(error -> "email".equals(error.getField()));

        if (isEmailError) {
            log.warn("이메일 형식 검증 실패");
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .success(false)
                    .errorCode(UserErrorCode.INVALID_EMAIL_FORMAT.getErrorCode())
                    .message(UserErrorCode.INVALID_EMAIL_FORMAT.getMessage())
                    .status(UserErrorCode.INVALID_EMAIL_FORMAT.getStatus().value())
                    .data(null)
                    .build();

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(errorResponse);
        }

        // 기타 유효성 검증 실패
        StringBuilder errorMessage = new StringBuilder("입력값 검증에 실패했습니다: ");
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errorMessage.append(fieldName).append(" - ").append(message).append("; ");
        });

        String finalMessage = errorMessage.toString().trim();
        log.warn("유효성 검증 실패: {}", finalMessage);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode(UserErrorCode.VALIDATION_FAILED.getErrorCode())
                .message(finalMessage)
                .status(UserErrorCode.VALIDATION_FAILED.getStatus().value())
                .data(null)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorResponse);
    }

    /**
     * 잘못된 HTTP 메서드 예외 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowedException(
            HttpRequestMethodNotSupportedException e) {
        log.warn("지원하지 않는 HTTP 메서드: {}", e.getMethod());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode(UserErrorCode.METHOD_NOT_ALLOWED.getErrorCode())
                .message(UserErrorCode.METHOD_NOT_ALLOWED.getMessage())
                .status(UserErrorCode.METHOD_NOT_ALLOWED.getStatus().value())
                .data(null)
                .build();

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(errorResponse);
    }

    /**
     * 기타 예외 처리 (서버 오류)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("예상치 못한 에러 발생: {}", e.getMessage(), e);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .success(false)
                .errorCode(UserErrorCode.SERVER_ERROR.getErrorCode())
                .message(UserErrorCode.SERVER_ERROR.getMessage())
                .status(UserErrorCode.SERVER_ERROR.getStatus().value())
                .data(null)
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse);
    }
}

