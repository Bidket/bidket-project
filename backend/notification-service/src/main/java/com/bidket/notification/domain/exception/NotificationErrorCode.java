package com.bidket.notification.domain.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 알림 서비스 에러 코드
 */
@Getter
public enum NotificationErrorCode {
    UNAUTHORIZED("N001", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("N002", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("N003", "만료된 토큰입니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("N004", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    NOTIFICATION_NOT_FOUND("N005", "알림을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR("N999", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String errorCode;
    private final String message;
    private final HttpStatus status;

    NotificationErrorCode(String errorCode, String message, HttpStatus status) {
        this.errorCode = errorCode;
        this.message = message;
        this.status = status;
    }
}

