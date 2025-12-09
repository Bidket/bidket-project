package com.bidket.user.application.service;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.infrastructure.persistence.entity.User;
import com.bidket.user.infrastructure.persistence.repository.UserRepository;
import com.bidket.user.presentation.dto.response.PermissionsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 권한 조회 서비스
 * 관리자가 특정 회원의 권한/역할 정보를 조회합니다.
 * 프론트에서 관리자 메뉴 노출 여부나 어드민 페이지 접근 제어에 활용됩니다.
 */
@Service
@RequiredArgsConstructor
public class PermissionsService {

    private final UserRepository userRepository;

    /**
     * 특정 회원의 권한 조회
     * 
     * @param memberId 조회할 회원 ID
     * @return 권한 조회 응답 (memberId, roles)
     * @throws UserException 사용자를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public PermissionsResponse getPermissions(UUID memberId) {
        // 사용자 조회
        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));
        
        // 현재는 기본 역할만 반환 (추후 User-Role 관계 구현 시 수정 필요)
        List<String> roles = Collections.singletonList("ROLE_USER");
        
        return PermissionsResponse.builder()
                .memberId(user.getId())
                .roles(roles)
                .build();
    }
}

