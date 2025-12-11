package com.bidket.auction.global.exception;

public class BidDomainException extends DomainException {

    public BidDomainException(BidErrorCode errorCode) {
        super(errorCode);
    }
}


