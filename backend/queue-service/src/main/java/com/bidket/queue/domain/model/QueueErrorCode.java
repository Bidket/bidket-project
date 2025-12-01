package com.bidket.queue.domain.model;

import com.bidket.common.presentation.error.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum QueueErrorCode implements BaseErrorCode {
    REDIS_CONNECTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Redis 연결 오류 발생"),
    REDIS_EXPIRE_SET_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "TTL 설정 중 오류 발생"),
    REDIS_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Redis 저장 실패");

    private final HttpStatus status;
    private final String message;

    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
