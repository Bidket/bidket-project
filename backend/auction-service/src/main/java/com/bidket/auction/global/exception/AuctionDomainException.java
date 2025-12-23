package com.bidket.auction.global.exception;

public class AuctionDomainException extends DomainException {

    public AuctionDomainException(AuctionErrorCode errorCode) {
        super(errorCode);
    }
}
