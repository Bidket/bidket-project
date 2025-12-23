package com.bidket.product.presentation.advice;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.product.domain.exception.ProductErrorCode;
import com.bidket.product.domain.exception.ProductException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ProductExceptionHandler {

    @ExceptionHandler(ProductException.class)
    public ResponseEntity<ApiResponse<?>> handleProductException(
            ProductException e
    ) {
        ProductErrorCode errorCode = e.getErrorCode();

        log.warn("[ProductException] {} - {}",
                errorCode.name(),
                errorCode.getMessage()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(
            Exception e
    ) {
        log.error("[Unhandled Exception]", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버 내부 오류가 발생했습니다."));
    }
}
