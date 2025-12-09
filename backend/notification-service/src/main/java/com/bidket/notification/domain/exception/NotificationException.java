package com.bidket.notification.domain.exception;

import lombok.Getter;

/**
 * 알림 서비스 예외
 */
@Getter
public class NotificationException extends RuntimeException {
    private final NotificationErrorCode errorCode;

    public NotificationException(NotificationErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public NotificationException(NotificationErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}

