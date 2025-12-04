package com.bidket.user.global.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 비밀번호 암호화 유틸리티
 * 
 * BCrypt 알고리즘을 사용하여 비밀번호를 해시화합니다.
 */
@Component
public class PasswordEncoder {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 비밀번호 암호화 (BCrypt 해시)
     * 
     * @param rawPassword 평문 비밀번호
     * @return 암호화된 비밀번호
     */
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 비밀번호 일치 여부 확인
     * 
     * @param rawPassword 평문 비밀번호
     * @param encodedPassword 암호화된 비밀번호
     * @return 일치하면 true, 그렇지 않으면 false
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}

