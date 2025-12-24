package com.bidket.user.global.security;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

/**
 * Authorization 헤더 검증 및 정규화 유틸리티
 * 검증과 전송에 동일한 정규화된 값을 사용하여 일관성 보장
 */
@Slf4j
public class AuthorizationTokenValidator {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Authorization 토큰 검증 및 정규화 (Bearer 형식 보장)
     * 검증과 전송에 동일한 정규화된 값을 사용하여 일관성 보장
     * @param authorizationToken 원본 Authorization 헤더 값
     * @return 정규화된 Authorization 헤더 값 (Bearer + 토큰)
     * @throws UserException 형식이 올바르지 않은 경우
     */
    public static String validateAndNormalize(String authorizationToken) {
        if (authorizationToken == null || authorizationToken.isBlank()) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }

        // trim으로 앞뒤 공백 제거
        String trimmed = authorizationToken.trim();

        // Bearer 형식 검증 (대소문자 구분 없이 체크, Locale.ROOT 사용하여 로케일 영향 방지)
        if (!trimmed.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            log.warn("Authorization 헤더 형식 오류: Bearer 형식이 아님");
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }

        // 정규화: "Bearer " + 토큰 (앞뒤 공백 제거된 토큰)
        // 토큰 추출: "bearer " (7자) 이후 부분 추출
        String token = trimmed.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            log.warn("Authorization 헤더 형식 오류: 토큰이 비어있음");
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }

        return BEARER_PREFIX + token;
    }
}

