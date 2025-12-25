package com.bidket.user.application.service;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.global.security.AuthenticationHelper;
import com.bidket.user.infrastructure.external.AuctionServiceClient;
import com.bidket.user.infrastructure.external.OrderServiceClient;
import com.bidket.user.presentation.dto.response.BidHistoryItemResponse;
import com.bidket.user.presentation.dto.response.BidHistoryResponse;
import com.bidket.user.presentation.dto.response.OrderHistoryItemResponse;
import com.bidket.user.presentation.dto.response.OrderHistoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 입찰/주문 내역 조회 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryService {

    private final AuctionServiceClient auctionServiceClient;
    private final OrderServiceClient orderServiceClient;

    /**
     * 입찰/주문 내역 조회
     * @param page 페이지 번호 (0부터 시작, 기본값 0)
     * @param size 페이지 사이즈 (기본값 20)
     * @param type 히스토리 타입 (BID 또는 ORDER, 기본값 BID)
     * @param authorizationToken Authorization 헤더 값 (Bearer 토큰)
     * @return 입찰/주문 내역 조회 응답 (타입에 따라 BidHistoryResponse 또는 OrderHistoryResponse)
     */
    public Object getHistory(Integer page, Integer size, String type, String authorizationToken) {
        UUID userId = AuthenticationHelper.getCurrentUserId();
        
        // 검증: page는 0 이상이어야 함
        if (page != null && page < 0) {
            throw new UserException(UserErrorCode.BAD_REQUEST);
        }
        
        // 검증: size는 1 이상 100 이하여야 함
        if (size != null && (size < 1 || size > 100)) {
            throw new UserException(UserErrorCode.BAD_REQUEST);
        }
        
        // 기본값 설정
        int pageNum = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0) ? size : 20;
        String historyType = (type != null && !type.isEmpty()) ? type : "BID";
        
        log.info("입찰/주문 내역 조회 요청: userId={}, type={}, page={}, size={}", userId, historyType, pageNum, pageSize);
        
        if ("BID".equalsIgnoreCase(historyType)) {
            return getBidHistory(userId, pageNum, pageSize, authorizationToken);
        } else if ("ORDER".equalsIgnoreCase(historyType)) {
            return getOrderHistory(userId, pageNum, pageSize, authorizationToken);
        } else {
            throw new UserException(UserErrorCode.INVALID_HISTORY_TYPE);
        }
    }

    /**
     * 입찰 내역 조회
     */
    private BidHistoryResponse getBidHistory(UUID userId, int page, int size, String authorizationToken) {
        try {
            AuctionServiceClient.BidListResponse bidListResponse = 
                    auctionServiceClient.getMyBids(userId, authorizationToken);
            
            List<AuctionServiceClient.BidResponse> bids = bidListResponse.getBids();
            
            // 페이징 처리
            int start = page * size;
            int end = Math.min(start + size, bids.size());
            List<AuctionServiceClient.BidResponse> pagedBids = 
                    start < bids.size() ? bids.subList(start, end) : List.of();
            
            // BidResponse를 BidHistoryItemResponse로 변환
            List<BidHistoryItemResponse> historyItems = pagedBids.stream()
                    .map(this::convertBidToHistoryItem)
                    .collect(Collectors.toList());
            
            long totalElements = bidListResponse.getTotalCount() != null ? bidListResponse.getTotalCount() : (long) bids.size();
            int totalPages = (int) Math.ceil((double) totalElements / size);
            
            return new BidHistoryResponse(
                    historyItems,
                    totalElements,
                    totalPages,
                    page,
                    size
            );
                    
        } catch (UserException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("입찰 내역 조회 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new UserException(UserErrorCode.AUCTION_SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("입찰 내역 조회 중 예상치 못한 오류: userId={}, error={}", userId, e.getMessage(), e);
            throw new UserException(UserErrorCode.SERVER_ERROR);
        }
    }

    /**
     * 주문 내역 조회
     */
    private OrderHistoryResponse getOrderHistory(UUID userId, int page, int size, String authorizationToken) {
        try {
            com.bidket.common.presentation.response.PageResponse<OrderServiceClient.OrderSummaryResponse> orderPage = 
                    orderServiceClient.getOrders(userId, page, size, authorizationToken);
            
            if (orderPage == null || orderPage.getContent() == null) {
                return new OrderHistoryResponse(
                        page,
                        size,
                        0L,
                        0,
                        false,
                        false,
                        List.of()
                );
            }
            
            // OrderSummaryResponse를 OrderHistoryItemResponse로 변환
            List<OrderHistoryItemResponse> historyItems = orderPage.getContent().stream()
                    .map(this::convertOrderToHistoryItem)
                    .collect(Collectors.toList());
            
            long totalElements = orderPage.getTotalElements();
            int totalPages = orderPage.getTotalPages();
            boolean hasNext = page < totalPages - 1;
            boolean hasPrevious = page > 0;
            
            return new OrderHistoryResponse(
                    page,
                    size,
                    totalElements,
                    totalPages,
                    hasNext,
                    hasPrevious,
                    historyItems
            );
                    
        } catch (UserException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("주문 내역 조회 실패: userId={}, error={}", userId, e.getMessage(), e);
            throw new UserException(UserErrorCode.ORDER_SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("주문 내역 조회 중 예상치 못한 오류: userId={}, error={}", userId, e.getMessage(), e);
            throw new UserException(UserErrorCode.SERVER_ERROR);
        }
    }

    /**
     * BidResponse를 BidHistoryItemResponse로 변환
     */
    private BidHistoryItemResponse convertBidToHistoryItem(
            AuctionServiceClient.BidResponse bid) {
        
        // auction-service의 BidResponse를 그대로 BidHistoryItemResponse로 변환
        return BidHistoryItemResponse.builder()
                .id(bid.getId())
                .auctionId(bid.getAuctionId())
                .bidderId(bid.getBidderId())
                .amount(bid.getAmount())
                .highest(bid.getHighest())
                .status(bid.getStatus())
                .rank(bid.getRank())
                .orderId(bid.getOrderId())
                .createdAt(bid.getCreatedAt())
                .build();
    }

    /**
     * OrderSummaryResponse를 OrderHistoryItemResponse로 변환
     */
    private OrderHistoryItemResponse convertOrderToHistoryItem(
            OrderServiceClient.OrderSummaryResponse order) {
        
        // order-service의 OrderSummaryResponse를 그대로 OrderHistoryItemResponse로 변환
        return OrderHistoryItemResponse.builder()
                .orderId(UUID.fromString(order.getOrderId()))
                .status(order.getStatus())
                .amount(order.getAmount())
                .productName(order.getProductName())
                .auctionTitle(order.getAuctionTitle())
                .auctionStartTime(order.getAuctionStartTime())
                .createdAt(order.getCreatedAt())
                .build();
    }
}

