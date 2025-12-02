package com.bidket.user.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 사용자 관련 에러 코드
 */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode {
    // 400 Bad Request
    TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "TERMS_NOT_AGREED", "이용약관에 동의해야 합니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "입력값 검증에 실패했습니다."),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_EMAIL_FORMAT", "잘못된 이메일 형식입니다."),
    WEAK_PASSWORD(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD", "비밀번호 강도가 부족합니다."),
    
    // 405 Method Not Allowed
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "이 요청 방식은 허용되지 않습니다."),
    
    // 409 Conflict
    EMAIL_DUPLICATE(HttpStatus.CONFLICT, "EMAIL_DUPLICATE", "이미 사용 중인 이메일입니다."),
    LOGIN_ID_DUPLICATE(HttpStatus.CONFLICT, "LOGIN_ID_DUPLICATE", "이미 사용 중인 로그인 아이디입니다."),
    NICKNAME_DUPLICATE(HttpStatus.CONFLICT, "NICKNAME_DUPLICATE", "이미 사용 중인 닉네임입니다."),
    
    // 500 Internal Server Error
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR", "서버에서 오류가 발생했습니다. 다시 시도해주세요.");

    private final HttpStatus status;
    private final String errorCode;
    private final String message;
}

