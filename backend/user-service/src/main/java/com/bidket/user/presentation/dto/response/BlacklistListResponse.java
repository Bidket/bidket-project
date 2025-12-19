package com.bidket.user.presentation.dto.response;

import lombok.Builder;

import java.util.List;

/**
 * 블랙리스트 목록 조회 응답 DTO (페이지네이션 포함)
 */
@Builder
public record BlacklistListResponse(
        /* 블랙리스트 회원 목록 */
        List<BlacklistItemResponse> content,
        
        /* 전체 건수 */
        Long totalElements,
        
        /* 전체 페이지 수 */
        Integer totalPages,
        
        /* 현재 페이지 번호 */
        Integer page,
        
        /* 페이지 사이즈 */
        Integer size
) {
}

