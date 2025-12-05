package com.bidket.user.application.service;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.domain.model.UserStatus;
import com.bidket.user.global.security.AuthenticationHelper;
import com.bidket.user.infrastructure.persistence.entity.User;
import com.bidket.user.infrastructure.persistence.entity.UserBlacklist;
import com.bidket.user.infrastructure.persistence.repository.UserBlacklistRepository;
import com.bidket.user.infrastructure.persistence.repository.UserRepository;
import com.bidket.user.presentation.dto.response.BidEligibilityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 입찰 가능 여부 체크 서비스
 * 현재 로그인한 회원의 입찰 가능 여부를 확인합니다.
 * 블랙리스트 여부, 회원 상태를 확인합니다.
 */
@Service
@RequiredArgsConstructor
public class BidEligibilityService {

    private final UserRepository userRepository;
    private final UserBlacklistRepository userBlacklistRepository;

    /**
     * 현재 로그인한 회원의 입찰 가능 여부 확인
     * @return 입찰 가능 여부 체크 응답 (eligible, reasons, memberStatus)
     * @throws UserException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public BidEligibilityResponse checkBidEligibility() {
        // SecurityContext에서 userId 추출
        UUID userId = AuthenticationHelper.getCurrentUserId();
        
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
              
        // 입찰 불가능 사유 리스트
        List<String> reasons = new ArrayList<>();
        
        // 1. 회원 상태 확인 (ACTIVE가 아니면 입찰 불가)
        if (user.getStatus() != UserStatus.ACTIVE) {
            reasons.add("INACTIVE_MEMBER");
        }
        
        // 2. 블랙리스트 확인
        LocalDateTime now = LocalDateTime.now();
        UserBlacklist blacklist = userBlacklistRepository.findActiveBlacklistByUserId(user.getId(), now)
                .orElse(null);
        
        if (blacklist != null) {
            reasons.add("BLACKLISTED");
        }
        
        
        // 입찰 가능 여부 판단 (사유가 없으면 가능)
        boolean eligible = reasons.isEmpty();
        
        return BidEligibilityResponse.builder()
                .eligible(eligible)
                .reasons(reasons)
                .memberStatus(user.getStatus().name())
                .build();
    }
}

