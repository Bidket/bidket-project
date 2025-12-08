package com.bidket.queue.presentation.advice;

import com.bidket.common.presentation.error.BaseErrorCode;
import com.bidket.common.presentation.error.CommonErrorCode;
import com.bidket.common.presentation.handler.GlobalExceptionHandler;
import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.queue.domain.exception.QueueException;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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

    @Override
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("에러 발생: {}", e.getMessage(), e);
        return ResponseEntity
                .status(CommonErrorCode.INTERNAL_ERROR.getStatus())
                .body(ApiResponse.error(CommonErrorCode.INTERNAL_ERROR.getMessage()));
    }


    @ExceptionHandler(ExpiredJwtException.class)
    public Mono<ResponseEntity<ApiResponse<?>>> handleExpiredJwtException(ExpiredJwtException e) {
        log.error("만료된 토큰입니다.\n현재 시간: {}\n만료 시간: {}",
                dateTimeFormat(Instant.now()),
                dateTimeFormat(e.getClaims().getExpiration().toInstant()));
        return Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.error(String.format("만료된 토큰입니다. 현재 시간: %s 만료 시간: %s",
                                dateTimeFormat(Instant.now()),
                                dateTimeFormat(e.getClaims().getExpiration().toInstant()))))
        );
    }

    private String dateTimeFormat(Instant instant) {
        final String datePattern = "yyyy-MM-dd HH:mm:ss.SSS";
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(datePattern, Locale.KOREA)
                .withZone(ZoneId.of("Asia/Seoul"));

        return dateTimeFormatter.format(instant);
    }
}
