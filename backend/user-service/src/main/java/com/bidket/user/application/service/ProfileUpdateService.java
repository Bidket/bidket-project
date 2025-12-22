package com.bidket.user.application.service;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.global.security.AuthenticationHelper;
import com.bidket.user.infrastructure.persistence.entity.User;
import com.bidket.user.infrastructure.persistence.repository.UserRepository;
import com.bidket.user.presentation.dto.request.ProfileUpdateRequest;
import com.bidket.user.presentation.dto.response.ProfileUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 프로필 수정 서비스
 */
@Service
@RequiredArgsConstructor
public class ProfileUpdateService {

    private final UserRepository userRepository;

    /**
     * RFC 5322 기반 이메일 형식 검증 패턴
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /**
     * 프로필 수정
     * 
     * @param request 프로필 수정 요청 정보
     * @return 프로필 수정 응답
     * @throws UserException 최소 1개 필드 미포함, 이메일/닉네임 중복, 이메일 형식 오류 시
     */
    @Transactional
    public ProfileUpdateResponse updateProfile(ProfileUpdateRequest request) {
        // Bean Validation에서 최소 1개 필드 검증이 이미 수행됨 (@AtLeastOneField)
        
        // 현재 로그인한 사용자 ID 추출
        UUID userId = AuthenticationHelper.getCurrentUserId();

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        // 닉네임 검증 및 업데이트
        String updatedNickname = null; // null이면 업데이트하지 않음
        if (request.nickname() != null && !request.nickname().isBlank()) {
            // 닉네임 형식 검증
            if (!isValidNicknameFormat(request.nickname())) {
                throw new UserException(UserErrorCode.VALIDATION_FAILED);
            }

            // 닉네임 중복 확인 (자기 자신 제외)
            if (userRepository.existsByNicknameAndIdNot(request.nickname(), userId)) {
                throw new UserException(UserErrorCode.NICKNAME_DUPLICATE);
            }

            updatedNickname = request.nickname();
        }

        // 전화번호 검증 및 업데이트 (하이픈 및 공백 제거)
        String updatedPhone = null; // null이면 업데이트하지 않음
        if (request.phone() != null && !request.phone().isBlank()) {
            // 하이픈과 공백 제거 (정규식에서 하이픈과 공백을 허용하지만, 저장 시에는 숫자만 저장)
            String normalizedPhone = request.phone()
                    .replaceAll("-", "")
                    .replaceAll("\\s", ""); // 모든 공백 문자 제거 (스페이스, 탭 등)
            // 빈 문자열이면 null로 정규화 (정규식에서 최소 1글자 검증하지만 방어적 처리)
            if (!normalizedPhone.isBlank()) {
                updatedPhone = normalizedPhone;
            }
        }

        // 이메일 검증 및 업데이트
        String updatedEmail = null; // null이면 업데이트하지 않음
        if (request.email() != null && !request.email().isBlank()) {
            // 이메일 형식 검증
            if (!isValidEmailFormat(request.email())) {
                throw new UserException(UserErrorCode.INVALID_EMAIL_FORMAT);
            }

            // 이메일 중복 확인 (자기 자신 제외)
            if (userRepository.existsByEmailAndIdNot(request.email(), userId)) {
                throw new UserException(UserErrorCode.EMAIL_DUPLICATE);
            }

            updatedEmail = request.email();
        }

        // 프로필 업데이트
        user.updateProfileFields(updatedNickname, updatedPhone, updatedEmail);

        // @Transactional + JPA 더티 체킹으로 자동 업데이트됨 (save() 불필요)
        // JPA Auditing으로 updatedAt 자동 설정됨

        // 응답 생성
        return ProfileUpdateResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .email(user.getEmail())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * 이메일 형식 검증
     * 
     * @param email 검증할 이메일 주소
     * @return 유효한 형식이면 true, 아니면 false
     */
    private boolean isValidEmailFormat(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        
        // 기본 길이 체크 (너무 긴 이메일 방지)
        if (email.length() > 255) {
            return false;
        }
        
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * 닉네임 형식 검증
     * 
     * @param nickname 검증할 닉네임
     * @return 유효한 형식이면 true, 아니면 false
     */
    private boolean isValidNicknameFormat(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return false;
        }
        
        // 기본 길이 체크 (2자 이상 100자 이하)
        if (nickname.length() < 2 || nickname.length() > 100) {
            return false;
        }
        
        // 한글, 영문, 숫자, 언더스코어, 하이픈 허용
        Pattern nicknamePattern = Pattern.compile("^[가-힣a-zA-Z0-9_-]+$");
        return nicknamePattern.matcher(nickname).matches();
    }
}

