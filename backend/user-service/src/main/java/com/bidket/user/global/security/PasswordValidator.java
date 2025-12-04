package com.bidket.user.global.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 비밀번호 강도 검증 유틸리티
 * 
 * 비밀번호 규칙:
 * - 최소 길이 8자 이상
 * - 대문자 또는 소문자 포함 (둘 중 하나만 있으면 됨)
 * - 숫자 포함
 * - 특수문자 최소 1개 이상 포함
 * - 공백 제외
 */
@Component
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final Pattern HAS_UPPER_OR_LOWER = Pattern.compile(".*[a-zA-Z].*");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern HAS_SPECIAL_CHAR = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
    private static final Pattern HAS_WHITESPACE = Pattern.compile(".*\\s.*");

    /**
     * 비밀번호 강도 검증
     * 
     * @param password 검증할 비밀번호
     * @return 검증 통과 시 true, 실패 시 false
     */
    public boolean isValid(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }

        // 공백 포함 여부 확인
        if (HAS_WHITESPACE.matcher(password).matches()) {
            return false;
        }

        // 대문자 또는 소문자 포함 여부 확인
        if (!HAS_UPPER_OR_LOWER.matcher(password).matches()) {
            return false;
        }

        // 숫자 포함 여부 확인
        if (!HAS_DIGIT.matcher(password).matches()) {
            return false;
        }

        // 특수문자 포함 여부 확인
        if (!HAS_SPECIAL_CHAR.matcher(password).matches()) {
            return false;
        }

        return true;
    }
}

