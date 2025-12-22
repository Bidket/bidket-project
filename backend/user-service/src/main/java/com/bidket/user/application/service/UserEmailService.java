package com.bidket.user.application.service;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.infrastructure.persistence.entity.User;
import com.bidket.user.infrastructure.persistence.repository.UserRepository;
import com.bidket.user.presentation.dto.response.UserEmailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 사용자 이메일 조회 서비스
 */
@Service
@RequiredArgsConstructor
public class UserEmailService {

    private final UserRepository userRepository;

    /**
     * 사용자 ID로 이메일 주소 조회
     * @param userId 조회할 사용자 ID
     * @return 사용자 이메일 응답
     * @throws UserException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public UserEmailResponse getUserEmail(UUID userId) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        
        // 응답 생성
        return UserEmailResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .build();
    }
}

