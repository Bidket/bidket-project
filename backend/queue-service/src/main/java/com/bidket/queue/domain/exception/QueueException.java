package com.bidket.queue.domain.exception;

import com.bidket.common.presentation.error.BaseErrorCode;
import lombok.Getter;

@Getter
public class QueueException extends RuntimeException {
    private final BaseErrorCode errorCode;

    public QueueException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
