package com.bidket.user.application.service;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.domain.model.Provider;
import com.bidket.user.global.security.AuthenticationHelper;
import com.bidket.user.global.security.PasswordEncoder;
import com.bidket.user.global.security.PasswordValidator;
import com.bidket.user.infrastructure.persistence.entity.User;
import com.bidket.user.infrastructure.persistence.repository.UserRepository;
import com.bidket.user.presentation.dto.request.PasswordChangeRequest;
import com.bidket.user.presentation.dto.response.PasswordChangeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 비밀번호 변경 서비스
 */
@Service
@RequiredArgsConstructor
public class PasswordChangeService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;

    /**
     * 비밀번호 변경
     * @param request 비밀번호 변경 요청 정보 (currentPassword, newPassword)
     * @return 비밀번호 변경 응답 (success, changedAt)
     * @throws UserException 현재 비밀번호 불일치, 새 비밀번호 강도 부족, 사용자 없음, 새 비밀번호가 현재 비밀번호와 동일한 경우
     */
    @Transactional
    public PasswordChangeResponse changePassword(PasswordChangeRequest request) {
        // 현재 로그인한 사용자 ID 추출
        UUID userId = AuthenticationHelper.getCurrentUserId();

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        // LOCAL provider 확인 (소셜 로그인 사용자는 비밀번호 변경 불가)
        if (user.getProvider() != Provider.LOCAL) {
            throw new UserException(UserErrorCode.PASSWORD_CHANGE_NOT_ALLOWED);
        }

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new UserException(UserErrorCode.INVALID_CURRENT_PASSWORD);
        }

        // 새 비밀번호가 현재 비밀번호와 동일한지 확인
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new UserException(UserErrorCode.VALIDATION_FAILED);
        }

        // 새 비밀번호 강도 검증
        if (!passwordValidator.isValid(request.newPassword())) {
            throw new UserException(UserErrorCode.WEAK_PASSWORD);
        }

        // 새 비밀번호 암호화
        String encodedNewPassword = passwordEncoder.encode(request.newPassword());

        // 비밀번호 업데이트
        user.updatePassword(encodedNewPassword);

        // @Transactional + JPA 더티 체킹으로 자동 업데이트됨 (save() 불필요)
        // JPA Auditing으로 updatedAt 자동 설정됨

        // 응답 생성 (DB에 저장된 변경 시각 사용)
        return PasswordChangeResponse.builder()
                .success(true)
                .changedAt(user.getUpdatedAt())
                .build();
    }
}

