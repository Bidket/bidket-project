package com.bidket.user.application.service;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.infrastructure.persistence.entity.User;
import com.bidket.user.infrastructure.persistence.entity.UserBlacklist;
import com.bidket.user.infrastructure.persistence.repository.UserBlacklistRepository;
import com.bidket.user.infrastructure.persistence.repository.UserRepository;
import com.bidket.user.presentation.dto.response.BlacklistStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 블랙리스트 상태 조회 서비스
 * 관리자가 특정 회원의 블랙리스트 여부를 조회합니다.
 * 입찰 가능 여부 판단 등에 활용됩니다.
 */
@Service
@RequiredArgsConstructor
public class BlacklistStatusService {

    private final UserRepository userRepository;
    private final UserBlacklistRepository userBlacklistRepository;

    /**
     * 특정 회원의 블랙리스트 상태 조회
     * @param memberId 조회할 회원 ID
     * @return 블랙리스트 상태 조회 응답 (memberId, isBlacklisted, reason, expireAt)
     * @throws UserException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public BlacklistStatusResponse getBlacklistStatus(UUID memberId) {
        // 사용자 조회
        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        
        // 활성화된 블랙리스트 조회
        LocalDateTime now = LocalDateTime.now();
        UserBlacklist blacklist = userBlacklistRepository.findActiveBlacklistByUserId(user.getId(), now)
                .orElse(null);
        
        if (blacklist != null) {
            // 블랙리스트에 등록된 경우
            return BlacklistStatusResponse.builder()
                    .memberId(user.getId())
                    .isBlacklisted(true)
                    .reason(blacklist.getReason())
                    .expireAt(blacklist.getExpireAt())
                    .build();
        } else {
            // 블랙리스트에 등록되지 않은 경우
            return BlacklistStatusResponse.builder()
                    .memberId(user.getId())
                    .isBlacklisted(false)
                    .reason(null)
                    .expireAt(null)
                    .build();
        }
    }
}

