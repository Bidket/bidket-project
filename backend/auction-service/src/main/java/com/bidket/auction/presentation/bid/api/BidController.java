package com.bidket.auction.presentation.bid.api;

import com.bidket.auction.application.bid.dto.request.CreateBidRequest;
import com.bidket.auction.application.bid.dto.response.BidListResponse;
import com.bidket.auction.application.bid.dto.response.BidResponse;
import com.bidket.auction.application.bid.service.BidService;
import com.bidket.common.presentation.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/bids")
@RequiredArgsConstructor
public class BidController {

    private final BidService bidService;

    /**
     * 입찰 생성
     * Gateway에서 Queue 검증 완료 후 X-Queue-Verified: true 헤더와 함께 요청
     *
     * @param userId 사용자 ID (Gateway에서 전달)
     * @param queueVerified Queue 검증 완료 여부 (Gateway에서 전달, true/false)
     * @param request 입찰 요청 데이터
     * @return 생성된 입찰 응답
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BidResponse>> createBid(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Queue-Verified", required = false, defaultValue = "false") boolean queueVerified,
            @Valid @RequestBody CreateBidRequest request
    ) {
        log.info("입찰 등록 요청 - 사용자: {}, 경매: {}, 금액: {}, Queue 검증: {}",
                userId, request.auctionId(), request.amount(), queueVerified);

        BidResponse response = bidService.createBid(userId, request, queueVerified);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("입찰이 성공적으로 등록되었습니다", response));
    }

    @GetMapping("/auction/{auctionId}")
    public ResponseEntity<ApiResponse<BidListResponse>> getBidsByAuction(
            @PathVariable UUID auctionId
    ) {
        log.info("경매별 입찰 목록 조회 - 경매 ID: {}", auctionId);
        
        BidListResponse response = bidService.getBidsByAuction(auctionId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<BidListResponse>> getMyBids(
            @RequestHeader("X-User-Id") UUID userId
    ) {
        log.info("내 입찰 목록 조회 - 사용자: {}", userId);
        
        BidListResponse response = bidService.getMyBids(userId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{bidId}")
    public ResponseEntity<ApiResponse<BidResponse>> getBid(
            @PathVariable UUID bidId
    ) {
            log.info("입찰 상세 조회 - 입찰 ID: {}", bidId);

            BidResponse response = bidService.getBidById(bidId);

            return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @DeleteMapping("/{bidId}")
    public ResponseEntity<ApiResponse<Void>> cancelBid(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID bidId
    ) {
        log.info("입찰 취소 요청 - 사용자: {}, 입찰 ID: {}", userId, bidId);
        
        bidService.cancelBid(bidId, userId);

        return ResponseEntity.ok(ApiResponse.success("입찰이 취소되었습니다", null));
    }

    /**
     * 즉시 구매
     * Gateway에서 Queue 검증 완료 후 X-Queue-Verified: true 헤더와 함께 요청
     *
     * @param userId 사용자 ID (Gateway에서 전달)
     * @param queueVerified Queue 검증 완료 여부 (Gateway에서 전달, true/false)
     * @param auctionId 경매 ID
     * @return 생성된 입찰 (즉시 구매)
     */
    @PostMapping("/auction/{auctionId}/buy-now")
    public ResponseEntity<ApiResponse<BidResponse>> buyNow(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Queue-Verified", required = false, defaultValue = "false") boolean queueVerified,
            @PathVariable UUID auctionId
    ) {
        log.info("즉시 구매 요청 - 사용자: {}, 경매: {}, Queue 검증: {}",
                userId, auctionId, queueVerified);

        var bid = bidService.buyNow(auctionId, userId, queueVerified);
        BidResponse response = BidResponse.from(bid);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("즉시 구매가 완료되었습니다", response));
    }
}


