package com.bidket.user.presentation.dto.response;

import java.util.List;

/**
 * 입찰 내역 조회 응답 DTO (페이지네이션 포함)
 */
public record BidHistoryResponse(
        /** 입찰 내역 목록 */
        List<BidHistoryItemResponse> content,
        
        /** 전체 건수 */
        Long totalElements,
        
        /** 전체 페이지 수 */
        Integer totalPages,
        
        /** 현재 페이지 번호 */
        Integer page,
        
        /** 페이지 사이즈 */
        Integer size
) {
}

