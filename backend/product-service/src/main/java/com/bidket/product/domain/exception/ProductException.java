package com.bidket.product.domain.exception;

import lombok.Getter;

@Getter
public class ProductException extends RuntimeException {

    private final ProductErrorCode errorCode;

    public ProductException(ProductErrorCode errorCode) {
        super(errorCode.getMessage()); // 예외 메시지 설정
        this.errorCode = errorCode;
    }
}
