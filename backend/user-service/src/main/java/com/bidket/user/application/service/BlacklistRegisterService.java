package com.bidket.user.application.service;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.infrastructure.persistence.entity.User;
import com.bidket.user.infrastructure.persistence.entity.UserBlacklist;
import com.bidket.user.infrastructure.persistence.repository.UserBlacklistRepository;
import com.bidket.user.infrastructure.persistence.repository.UserRepository;
import com.bidket.user.presentation.dto.request.BlacklistRegisterRequest;
import com.bidket.user.presentation.dto.response.BlacklistRegisterResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 블랙리스트 등록 서비스
 * 관리자가 특정 회원을 블랙리스트로 등록합니다.
 * 입찰 가능 여부 체크 시 사용
 */
@Service
@RequiredArgsConstructor
public class BlacklistRegisterService {

    private final UserRepository userRepository;
    private final UserBlacklistRepository userBlacklistRepository;

    /**
     * 블랙리스트로 등록
     * @param memberId 블랙리스트로 등록할 회원 ID
     * @param request 블랙리스트 등록 요청 정보 (reason, expireAt)
     * @return 블랙리스트 등록 응답 (memberId, blacklisted, reason, expireAt, createdAt)
     * @throws UserException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public BlacklistRegisterResponse registerBlacklist(UUID memberId, BlacklistRegisterRequest request) {
        // 사용자 조회
        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        
        // 기존 활성화된 블랙리스트가 있는지 확인
        LocalDateTime now = LocalDateTime.now();
        UserBlacklist existingBlacklist = userBlacklistRepository.findActiveBlacklistByUserId(user.getId(), now)
                .orElse(null);
        
        if (existingBlacklist != null) {
            // 기존 블랙리스트가 있으면 기존 것을 비활성화하고 새로 등록
            existingBlacklist.deactivate();
            userBlacklistRepository.save(existingBlacklist);
        }
        
        // 새로운 블랙리스트 등록
        UserBlacklist blacklist = UserBlacklist.builder()
                .userId(user.getId())
                .reason(request.reason())
                .expireAt(request.expireAt())
                .active(true)
                .build();
        
        UserBlacklist savedBlacklist = userBlacklistRepository.save(blacklist);

        LocalDateTime createdAt = savedBlacklist.getCreatedAt() != null 
                ? savedBlacklist.getCreatedAt() 
                : LocalDateTime.now();
        
        return BlacklistRegisterResponse.builder()
                .memberId(user.getId())
                .blacklisted(true)
                .reason(savedBlacklist.getReason())
                .expireAt(savedBlacklist.getExpireAt())
                .createdAt(createdAt)
                .build();
    }
}

