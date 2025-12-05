package com.bidket.user.application.service;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.domain.model.PointHistoryType;
import com.bidket.user.global.security.AuthenticationHelper;
import com.bidket.user.infrastructure.persistence.entity.PointHistory;
import com.bidket.user.infrastructure.persistence.repository.PointHistoryRepository;
import com.bidket.user.presentation.dto.response.PointHistoryItemResponse;
import com.bidket.user.presentation.dto.response.PointHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 포인트 히스토리 조회 서비스
 */
@Service
@RequiredArgsConstructor
public class PointHistoryService {

    private final PointHistoryRepository pointHistoryRepository;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int DEFAULT_PAGE = 0;

    /**
     * 현재 로그인한 사용자의 포인트 히스토리 조회
     * @param page 페이지 번호 (0부터 시작, 기본값 0)
     * @param size 페이지 사이즈 (기본값 20)
     * @param type 필터용 타입 (CHARGE, USE, REFUND, CANCEL 등, 선택사항)
     * @return 포인트 히스토리 조회 응답 (페이지네이션 포함)
     */
    @Transactional(readOnly = true)
    public PointHistoryResponse getPointHistory(Integer page, Integer size, String type) {
        // SecurityContext에서 userId 추출
        UUID userId = AuthenticationHelper.getCurrentUserId();
        
        // 페이지네이션 파라미터 설정
        int pageNumber = (page != null && page >= 0) ? page : DEFAULT_PAGE;
        int pageSize = (size != null && size > 0) ? size : DEFAULT_PAGE_SIZE;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        
        // 타입 필터 파싱
        PointHistoryType historyType = null;
        if (type != null && !type.trim().isEmpty()) {
            try {
                historyType = PointHistoryType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new UserException(UserErrorCode.BAD_REQUEST);
            }
        }
        
        // 포인트 히스토리 조회
        Page<PointHistory> historyPage;
        if (historyType != null) {
            historyPage = pointHistoryRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, historyType, pageable);
        } else {
            historyPage = pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        
        // 응답 DTO 변환
        List<PointHistoryItemResponse> content = historyPage.getContent().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());
        
        return PointHistoryResponse.builder()
                .content(content)
                .totalElements(historyPage.getTotalElements())
                .totalPages(historyPage.getTotalPages())
                .page(historyPage.getNumber())
                .size(historyPage.getSize())
                .build();
    }

    /**
     * PointHistory 엔티티를 PointHistoryItemResponse로 변환
     */
    private PointHistoryItemResponse toItemResponse(PointHistory history) {
        // amount가 USE 타입인 경우 음수로 변환 (API 스펙에 따르면 음수: 사용)
        Long amount = history.getAmount();
        if (history.getType() == PointHistoryType.USE) {
            amount = -Math.abs(amount);
        }
        
        return PointHistoryItemResponse.builder()
                .historyId(history.getId())
                .type(history.getType().name())
                .amount(amount)
                .balanceAfter(history.getBalanceAfter())
                .description(history.getDescription())
                .relatedAuctionId(null) // 현재 엔티티에 auction_id 필드가 없음 (추후 추가 가능)
                .relatedOrderId(history.getOrderId())
                .createdAt(history.getCreatedAt())
                .build();
    }
}

