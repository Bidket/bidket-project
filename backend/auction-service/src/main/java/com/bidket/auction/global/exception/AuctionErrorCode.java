package com.bidket.auction.global.exception;

import com.bidket.common.presentation.error.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuctionErrorCode implements BaseErrorCode {

    AUCTION_NOT_FOUND(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다."),
    
    INVALID_START_PRICE(HttpStatus.BAD_REQUEST, "시작가는 10,000원 이상이어야 합니다."),
    INVALID_BID_INCREMENT(HttpStatus.BAD_REQUEST, "입찰 단위는 1,000원 이상이어야 합니다."),
    INVALID_BUY_NOW_PRICE(HttpStatus.BAD_REQUEST, "즉시구매가는 시작가보다 커야 합니다."),
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "시작 시간은 현재 + 1시간 이후, 종료 시간은 시작 + 1시간 이후여야 합니다."),
    INVALID_AUCTION_PERIOD(HttpStatus.BAD_REQUEST, "경매 기간은 최대 7일까지 가능합니다."),
    
    INVALID_AUCTION_STATUS(HttpStatus.CONFLICT, "경매 상태가 올바르지 않습니다."),
    AUCTION_NOT_ACTIVE(HttpStatus.CONFLICT, "진행 중인 경매가 아닙니다."),
    AUCTION_ALREADY_STARTED(HttpStatus.CONFLICT, "이미 시작된 경매는 수정할 수 없습니다."),
    AUCTION_ALREADY_ENDED(HttpStatus.CONFLICT, "이미 종료된 경매입니다."),
    CANNOT_CANCEL_WITH_BIDS(HttpStatus.CONFLICT, "입찰이 있는 경매는 취소할 수 없습니다."),
    
    BID_AMOUNT_TOO_LOW(HttpStatus.BAD_REQUEST, "입찰 금액이 최소 입찰가보다 낮습니다."),
    SELF_BID_NOT_ALLOWED(HttpStatus.FORBIDDEN, "본인의 경매에는 입찰할 수 없습니다."),
    
    MAX_EXTENSIONS_REACHED(HttpStatus.CONFLICT, "최대 연장 횟수(3회)를 초과했습니다."),
    
    STOCK_NOT_AVAILABLE(HttpStatus.CONFLICT, "재고가 없습니다."),
    STOCK_RESERVATION_FAILED(HttpStatus.CONFLICT, "재고 예약에 실패했습니다."),
    
    NOT_AUCTION_OWNER(HttpStatus.FORBIDDEN, "경매 소유자만 수정/취소할 수 있습니다."),
    
    OPTIMISTIC_LOCK_FAILURE(HttpStatus.CONFLICT, "다른 사용자가 이미 입찰했습니다. 다시 시도해주세요."),
    
    AUCTION_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "경매 생성에 실패했습니다."),
    AUCTION_UPDATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "경매 수정에 실패했습니다.")
    ;

    private final HttpStatus status;
    private final String message;
}
