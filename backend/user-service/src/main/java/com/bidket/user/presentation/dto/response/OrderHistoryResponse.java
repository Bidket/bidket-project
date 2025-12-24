package com.bidket.user.presentation.dto.response;

import java.util.List;

/**
 * 주문 내역 조회 응답 DTO (페이지네이션 포함)
 */
public record OrderHistoryResponse(
        /** 현재 페이지 번호 */
        Integer page,
        
        /** 페이지 사이즈 */
        Integer size,
        
        /** 전체 건수 */
        Long totalElements,
        
        /** 전체 페이지 수 */
        Integer totalPages,
        
        /** 다음 페이지 존재 여부 */
        Boolean hasNext,
        
        /** 이전 페이지 존재 여부 */
        Boolean hasPrevious,
        
        /** 주문 내역 목록 */
        List<OrderHistoryItemResponse> content
) {
}

