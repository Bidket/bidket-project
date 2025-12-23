package com.bidket.user.application.service;

import com.bidket.user.infrastructure.persistence.repository.UserRepository;
import com.bidket.user.presentation.dto.response.NicknameCheckResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * 닉네임 중복 체크 서비스
 * 회원가입 화면에서 실시간 중복 체크용으로 사용됩니다.
 */
@Service
@RequiredArgsConstructor
public class NicknameCheckService {

    private final UserRepository userRepository;

    /**
     * 닉네임 형식 검증 패턴
     * - 한글, 영문, 숫자, 언더스코어, 하이픈 허용
     * - 2자 이상 20자 이하
     */
    private static final Pattern NICKNAME_PATTERN = Pattern.compile(
            "^[가-힣a-zA-Z0-9_-]{2,20}$"
    );

    /**
     * 닉네임 중복 체크
     * @param nickname 확인할 닉네임
     * @return 닉네임 중복 체크 응답 (nickname, available, reason)
     */
    @Transactional(readOnly = true)
    public NicknameCheckResponse checkNickname(String nickname) {
        // 닉네임 형식 검증
        if (nickname == null || nickname.isBlank() || !isValidNicknameFormat(nickname)) {
            return NicknameCheckResponse.builder()
                    .nickname(nickname)
                    .available(false)
                    .reason("INVALID_FORMAT")
                    .build();
        }

        // 닉네임 중복 확인
        boolean exists = userRepository.existsByNickname(nickname);
        
        if (exists) {
            return NicknameCheckResponse.builder()
                    .nickname(nickname)
                    .available(false)
                    .reason("ALREADY_USED")
                    .build();
        }

        return NicknameCheckResponse.builder()
                .nickname(nickname)
                .available(true)
                .reason(null)
                .build();
    }

    /**
     * 닉네임 형식 검증
     * @param nickname 검증할 닉네임
     * @return 유효한 형식이면 true, 아니면 false
     */
    private boolean isValidNicknameFormat(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return false;
        }
        
        // 기본 길이 체크 (2자 이상 20자 이하)
        if (nickname.length() < 2 || nickname.length() > 20) {
            return false;
        }
        
        return NICKNAME_PATTERN.matcher(nickname).matches();
    }
}

