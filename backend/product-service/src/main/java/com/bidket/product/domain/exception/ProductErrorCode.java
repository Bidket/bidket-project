package com.bidket.product.domain.exception;

import com.bidket.common.presentation.error.BaseErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum ProductErrorCode implements BaseErrorCode {

    PRODUCT_CATEGORY_NOT_SET(HttpStatus.BAD_REQUEST, "상품에 설정된 카테고리가 없습니다."),
    SKU_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "사용할 수 없는 SKU 입니다."),

    PRODUCT_TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "상품타입을 찾을 수 없습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    SIZE_TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "사이즈타입을 찾을 수 없습니다."),
    BRAND_NOT_FOUND(HttpStatus.NOT_FOUND, "브랜드를 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    SIZE_NOT_FOUND(HttpStatus.NOT_FOUND, "사이즈를 찾을 수 없습니다."),
    SKU_NOT_FOUND(HttpStatus.NOT_FOUND, "SKU를 찾을 수 없습니다."),
    SHOES_DETAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "신발 상품을 찾을 수 없습니다."),

    BRAND_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 브랜드입니다."),
    SKU_CODE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 SKU code입니다."),
    ;

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
