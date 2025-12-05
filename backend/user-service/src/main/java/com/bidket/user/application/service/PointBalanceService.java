package com.bidket.user.application.service;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.global.security.AuthenticationHelper;
import com.bidket.user.infrastructure.persistence.entity.PointAccount;
import com.bidket.user.infrastructure.persistence.repository.PointAccountRepository;
import com.bidket.user.presentation.dto.response.PointBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 포인트 잔액 조회 서비스
 */
@Service
@RequiredArgsConstructor
public class PointBalanceService {

    private final PointAccountRepository pointAccountRepository;
    private static final String DEFAULT_CURRENCY = "POINT";

    /**
     * 현재 로그인한 사용자의 포인트 잔액 조회
     * 
     * @return 포인트 잔액 조회 응답
     * @throws UserException 포인트 계정을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public PointBalanceResponse getPointBalance() {
        // SecurityContext에서 userId 추출
        UUID userId = AuthenticationHelper.getCurrentUserId();
        
        // 포인트 계정 조회
        PointAccount pointAccount = pointAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        
        // 응답 생성
        return PointBalanceResponse.builder()
                .memberId(pointAccount.getUserId())
                .balance(pointAccount.getBalance())
                .currency(DEFAULT_CURRENCY)
                .updatedAt(pointAccount.getUpdatedAt())
                .build();
    }
}

