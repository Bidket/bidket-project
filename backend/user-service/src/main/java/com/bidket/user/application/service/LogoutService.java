package com.bidket.user.application.service;

import com.bidket.user.global.security.AuthenticationHelper;
import com.bidket.user.infrastructure.persistence.repository.RefreshTokenRepository;
import com.bidket.user.presentation.dto.response.LogoutResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 로그아웃 서비스
 * 1. Authorization 헤더에서 사용자 ID 추출 (SecurityContext)
 * 2. 해당 userId에 연결된 모든 Refresh Token 삭제
 * 3. 멱등성 보장: 이미 삭제된 상태여도 성공 응답 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogoutService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 로그아웃 처리
     * Authorization 헤더만으로 사용자 식별하여 해당 사용자의 모든 Refresh Token을 삭제합니다.
     * @return 로그아웃 응답 (success)
     */
    @Transactional
    public LogoutResponse logout() {
        // 1. Authorization 헤더에서 사용자 ID 추출 (SecurityContext)
        UUID userId = AuthenticationHelper.getCurrentUserId();

        // 2. 해당 userId에 연결된 모든 Refresh Token 삭제
        refreshTokenRepository.deleteByUserId(userId);

        // 멱등성 보장: 이미 삭제된 상태여도 성공 응답 반환
        return new LogoutResponse(true);
    }
}

