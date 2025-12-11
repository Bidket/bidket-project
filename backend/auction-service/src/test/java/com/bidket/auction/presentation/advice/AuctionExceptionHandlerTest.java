package com.bidket.auction.presentation.advice;

import com.bidket.auction.global.exception.AuctionDomainException;
import com.bidket.auction.global.exception.AuctionErrorCode;
import com.bidket.auction.global.exception.BidDomainException;
import com.bidket.auction.global.exception.BidErrorCode;
import com.bidket.common.presentation.response.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuctionExceptionHandler 도메인 예외 매핑 테스트")
class AuctionExceptionHandlerTest {

    private final AuctionExceptionHandler handler = new AuctionExceptionHandler();

    @Test
    @DisplayName("AuctionDomainException → 정의된 상태코드/메시지 반환")
    void handleAuctionDomainException() {
        // Given
        AuctionDomainException ex = new AuctionDomainException(AuctionErrorCode.AUCTION_NOT_FOUND);

        // When
        ResponseEntity<ApiResponse<?>> response = handler.handleDomainException(ex);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(AuctionErrorCode.AUCTION_NOT_FOUND.getStatus());
        ApiResponse<?> body = response.getBody();
        assertThat(body).isNotNull();
        ApiResponse<?> nonNullBody = java.util.Objects.requireNonNull(body);
        assertThat(nonNullBody.isSuccess()).isFalse();
        assertThat(nonNullBody.getMessage()).isEqualTo(AuctionErrorCode.AUCTION_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("BidDomainException → 정의된 상태코드/메시지 반환")
    void handleBidDomainException() {
        // Given
        BidDomainException ex = new BidDomainException(BidErrorCode.BID_NOT_FOUND);

        // When
        ResponseEntity<ApiResponse<?>> response = handler.handleDomainException(ex);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(BidErrorCode.BID_NOT_FOUND.getStatus());
        ApiResponse<?> body = response.getBody();
        assertThat(body).isNotNull();
        ApiResponse<?> nonNullBody = java.util.Objects.requireNonNull(body);
        assertThat(nonNullBody.isSuccess()).isFalse();
        assertThat(nonNullBody.getMessage()).isEqualTo(BidErrorCode.BID_NOT_FOUND.getMessage());
    }
}


