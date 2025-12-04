package com.bidket.user.application.service;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.infrastructure.persistence.entity.PointAccount;
import com.bidket.user.infrastructure.persistence.repository.PointAccountRepository;
import com.bidket.user.presentation.dto.response.PointBalanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 포인트 잔액 조회 서비스
 */
@Service
@RequiredArgsConstructor
public class PointBalanceService {

    private final PointAccountRepository pointAccountRepository;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
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
        UUID userId = getCurrentUserId();
        
        // 포인트 계정 조회
        PointAccount pointAccount = pointAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        
        // updatedAt을 ISO-8601 형식으로 변환
        String updatedAt = pointAccount.getUpdatedAt() != null
                ? pointAccount.getUpdatedAt().atOffset(ZoneOffset.UTC).format(ISO_FORMATTER)
                : null;
        
        // 응답 생성
        return PointBalanceResponse.builder()
                .memberId(pointAccount.getUserId())
                .balance(pointAccount.getBalance())
                .currency(DEFAULT_CURRENCY)
                .updatedAt(updatedAt)
                .build();
    }

    /**
     * SecurityContext에서 현재 사용자 ID 추출
     * 
     * @return 사용자 ID
     * @throws UserException 인증 정보가 없는 경우
     */
    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
        
        try {
            if (authentication.getPrincipal() instanceof UUID) {
                return (UUID) authentication.getPrincipal();
            } else if (authentication.getPrincipal() instanceof String) {
                return UUID.fromString((String) authentication.getPrincipal());
            } else {
                throw new UserException(UserErrorCode.UNAUTHORIZED);
            }
        } catch (IllegalArgumentException e) {
            // UUID 형식이 잘못된 경우
            throw new UserException(UserErrorCode.INVALID_TOKEN);
        } catch (Exception e) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
    }
}

