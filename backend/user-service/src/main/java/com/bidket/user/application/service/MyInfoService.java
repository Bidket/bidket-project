package com.bidket.user.application.service;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.global.security.AuthenticationHelper;
import com.bidket.user.infrastructure.persistence.entity.User;
import com.bidket.user.infrastructure.persistence.repository.UserRepository;
import com.bidket.user.presentation.dto.response.MyInfoResponse;
import lombok.RequiredArgsConstructor;
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
        UUID userId = AuthenticationHelper.getCurrentUserId();
        
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
}

