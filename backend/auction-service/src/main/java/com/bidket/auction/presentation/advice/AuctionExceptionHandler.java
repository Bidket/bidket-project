package com.bidket.auction.presentation.advice;

import com.bidket.auction.global.exception.DomainException;
import com.bidket.common.presentation.error.BaseErrorCode;
import com.bidket.common.presentation.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.bidket.auction.presentation")
public class AuctionExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<?>> handleDomainException(DomainException e) {
        BaseErrorCode errorCode = e.getErrorCode();
        log.warn("도메인 예외: {}", errorCode.getMessage(), e);
        return ResponseEntity
                .status(errorCode.getStatus().value())
                .body(ApiResponse.error(errorCode.getMessage()));
    }

}
