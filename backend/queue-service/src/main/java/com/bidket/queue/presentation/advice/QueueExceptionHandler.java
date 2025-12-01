package com.bidket.queue.presentation.advice;

import com.bidket.common.presentation.error.BaseErrorCode;
import com.bidket.common.presentation.handler.GlobalExceptionHandler;
import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.queue.domain.exception.QueueException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class QueueExceptionHandler extends GlobalExceptionHandler {

    @ExceptionHandler(QueueException.class)
    public Mono<ResponseEntity<ApiResponse<?>>> handleQueueException(QueueException e) {
        BaseErrorCode errorCode = e.getErrorCode();
        log.error("대기열 서비스 에러 발생: {}", e.getMessage(), e);
        return Mono.just(ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getMessage())));
    }
}
