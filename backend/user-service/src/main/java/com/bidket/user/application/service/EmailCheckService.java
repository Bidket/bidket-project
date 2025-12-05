package com.bidket.user.application.service;

import com.bidket.user.infrastructure.persistence.repository.UserRepository;
import com.bidket.user.presentation.dto.response.EmailCheckResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * 이메일 중복 체크 서비스
 * 
 * 회원가입 화면에서 실시간 중복 체크용으로 사용됩니다.
 */
@Service
@RequiredArgsConstructor
public class EmailCheckService {

    private final UserRepository userRepository;

    /**
     * RFC 5322 기반 이메일 형식 검증 패턴
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /**
     * 이메일 중복 체크
     * 
     * @param email 확인할 이메일 주소
     * @return 이메일 중복 체크 응답 (email, available, reason)
     */
    @Transactional(readOnly = true)
    public EmailCheckResponse checkEmail(String email) {
        // 이메일 형식 검증
        if (email == null || email.isBlank() || !isValidEmailFormat(email)) {
            return EmailCheckResponse.builder()
                    .email(email)
                    .available(false)
                    .reason("INVALID_FORMAT")
                    .build();
        }

        // 이메일 중복 확인
        boolean exists = userRepository.existsByEmail(email);
        
        if (exists) {
            return EmailCheckResponse.builder()
                    .email(email)
                    .available(false)
                    .reason("ALREADY_USED")
                    .build();
        }

        return EmailCheckResponse.builder()
                .email(email)
                .available(true)
                .reason(null)
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
}

