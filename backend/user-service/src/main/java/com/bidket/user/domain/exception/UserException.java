package com.bidket.user.domain.exception;

import lombok.Getter;

/**
 * 사용자 관련 커스텀 예외
 */
@Getter
public class UserException extends RuntimeException {
    private final UserErrorCode errorCode;

    public UserException(UserErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}

