package com.bidket.user.application.service;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.global.security.AuthenticationHelper;
import com.bidket.user.infrastructure.persistence.entity.User;
import com.bidket.user.infrastructure.persistence.repository.UserRepository;
import com.bidket.user.presentation.dto.request.MemberDeactivationRequest;
import com.bidket.user.presentation.dto.response.MemberDeactivationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 회원 탈퇴(비활성화) 서비스
 */
@Service
@RequiredArgsConstructor
public class MemberDeactivationService {

    private final UserRepository userRepository;

    /**
     * 회원 탈퇴(비활성화) 처리
     * 논리삭제 방식으로 상태를 WITHDRAWN으로 변경
     * @param request 회원 탈퇴 요청 정보 (reason - 선택사항)
     * @return 회원 탈퇴 응답 (memberId, status, deactivatedAt)
     * @throws UserException 사용자를 찾을 수 없는 경우
     */
    @Transactional
    public MemberDeactivationResponse deactivateMember(MemberDeactivationRequest request) {
        // 현재 로그인한 사용자 ID 추출
        UUID userId = AuthenticationHelper.getCurrentUserId();

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        // 이미 탈퇴한 사용자인지 확인
        if (user.getStatus() == com.bidket.user.domain.model.UserStatus.WITHDRAWN) {
            throw new UserException(UserErrorCode.ALREADY_WITHDRAWN);
        }

        // 비활성화 처리 (상태를 WITHDRAWN으로 변경 및 deactivatedAt 설정)
        user.withdraw();

        // @Transactional + JPA 더티 체킹으로 자동 업데이트됨 (save() 불필요)
        // JPA Auditing으로 updatedAt 자동 설정됨

        // 응답 생성 (user에 저장된 값을 그대로 사용)
        return MemberDeactivationResponse.builder()
                .memberId(user.getId())
                .status(user.getStatus())
                .deactivatedAt(user.getDeactivatedAt())
                .build();
    }
}

