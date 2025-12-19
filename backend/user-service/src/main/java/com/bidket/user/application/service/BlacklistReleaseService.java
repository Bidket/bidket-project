package com.bidket.user.application.service;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.infrastructure.persistence.entity.User;
import com.bidket.user.infrastructure.persistence.entity.UserBlacklist;
import com.bidket.user.infrastructure.persistence.repository.UserBlacklistRepository;
import com.bidket.user.infrastructure.persistence.repository.UserRepository;
import com.bidket.user.presentation.dto.response.BlacklistReleaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 블랙리스트 해제 서비스
 * 관리자가 특정 회원의 블랙리스트를 해제합니다.
 */
@Service
@RequiredArgsConstructor
public class BlacklistReleaseService {

    private final UserRepository userRepository;
    private final UserBlacklistRepository userBlacklistRepository;

    /**
     * 블랙리스트 해제
     * @param memberId 블랙리스트를 해제할 회원 ID
     * @return 블랙리스트 해제 응답 (memberId, blacklisted, updatedAt)
     * @throws UserException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public BlacklistReleaseResponse releaseBlacklist(UUID memberId) {
        // 사용자 조회
        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        
        // 활성화된 블랙리스트 조회
        LocalDateTime now = LocalDateTime.now();
        Optional<UserBlacklist> activeBlacklistOpt = userBlacklistRepository.findActiveBlacklistByUserId(user.getId(), now);
        
        if (activeBlacklistOpt.isEmpty()) {
            // 활성화된 블랙리스트가 없는 경우, 이미 해제되었는지 확인
            Optional<UserBlacklist> latestBlacklistOpt = userBlacklistRepository.findFirstByUserIdOrderByCreatedAtDesc(user.getId());
            
            if (latestBlacklistOpt.isPresent() && !latestBlacklistOpt.get().getActive()) {
                // 블랙리스트는 있지만 이미 비활성화된 경우
                throw new UserException(UserErrorCode.BLACKLIST_ALREADY_RELEASED);
            } else {
                // 블랙리스트가 아예 없는 경우
                throw new UserException(UserErrorCode.BLACKLIST_NOT_FOUND);
            }
        }
        
        UserBlacklist activeBlacklist = activeBlacklistOpt.get();
        
        // 블랙리스트 비활성화
        activeBlacklist.deactivate();
        // saveAndFlush를 사용하여 즉시 DB에 반영하고 Auditing 필드(updatedAt) 업데이트 보장
        UserBlacklist savedBlacklist = userBlacklistRepository.saveAndFlush(activeBlacklist);
        
        // updatedAt 가져오기 (BaseEntity의 JPA Auditing으로 자동 설정됨)
        // saveAndFlush 후에는 반드시 값이 설정되어 있음
        LocalDateTime updatedAt = savedBlacklist.getUpdatedAt();
        
        return BlacklistReleaseResponse.builder()
                .memberId(user.getId())
                .blacklisted(false)
                .updatedAt(updatedAt)
                .build();
    }
}

