package com.bidket.user.application.service;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.infrastructure.persistence.entity.User;
import com.bidket.user.infrastructure.persistence.repository.UserRepository;
import com.bidket.user.presentation.dto.response.MyInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 내 정보 조회 서비스
 */
@Service
@RequiredArgsConstructor
public class MyInfoService {

    private final UserRepository userRepository;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * 현재 로그인한 사용자의 정보 조회
     * 
     * @return 내 정보 응답
     * @throws UserException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public MyInfoResponse getMyInfo() {
        // SecurityContext에서 userId 추출
        UUID userId = getCurrentUserId();
        
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        
        // createdAt을 ISO-8601 형식으로 변환
        String createdAt = user.getCreatedAt() != null
                ? user.getCreatedAt().atOffset(ZoneOffset.UTC).format(ISO_FORMATTER)
                : null;
        
        // 응답 생성
        return MyInfoResponse.builder()
                .memberId(user.getId())
                .loginId(user.getLoginId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role("ROLE_USER") // 기본 권한 (추후 User-Role 관계 구현 시 수정 필요)
                .createdAt(createdAt)
                .status(user.getStatus().name())
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

