package com.bidket.auction.global.exception;

import com.bidket.common.presentation.error.BaseErrorCode;
import lombok.Getter;

@Getter
public abstract class DomainException extends RuntimeException {

    private final BaseErrorCode errorCode;

    protected DomainException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
