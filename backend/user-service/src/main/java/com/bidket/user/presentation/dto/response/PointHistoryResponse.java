package com.bidket.user.presentation.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * 포인트 히스토리 조회 응답 DTO (페이지네이션 포함)
 */
@Builder
public record PointHistoryResponse(
        /** 포인트 히스토리 목록 */
        List<PointHistoryItemResponse> content,
        
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

