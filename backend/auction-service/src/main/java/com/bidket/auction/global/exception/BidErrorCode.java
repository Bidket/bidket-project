package com.bidket.auction.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BidErrorCode {
    
    // 400 Bad Request
    INVALID_BID_AMOUNT(HttpStatus.BAD_REQUEST, "BID_001", "유효하지 않은 입찰 금액입니다"),
    BID_AMOUNT_TOO_LOW(HttpStatus.BAD_REQUEST, "BID_002", "입찰 금액이 최소 입찰가보다 낮습니다"),
    SELF_BID_NOT_ALLOWED(HttpStatus.FORBIDDEN, "BID_003", "본인의 경매에는 입찰할 수 없습니다"),
    
    // 404 Not Found
    BID_NOT_FOUND(HttpStatus.NOT_FOUND, "BID_404", "입찰을 찾을 수 없습니다"),
    
    // 409 Conflict
    BID_CONFLICT(HttpStatus.CONFLICT, "BID_409", "입찰 처리 중 충돌이 발생했습니다"),
    AUCTION_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "BID_410", "진행 중인 경매가 아닙니다"),
    AUCTION_EXPIRED(HttpStatus.BAD_REQUEST, "BID_411", "종료된 경매입니다"),
    
    // 403 Forbidden
    CANNOT_CANCEL_HIGHEST_BID(HttpStatus.FORBIDDEN, "BID_403", "최고가 입찰은 취소할 수 없습니다"),
    NOT_BID_OWNER(HttpStatus.FORBIDDEN, "BID_404", "본인의 입찰만 취소할 수 있습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}

