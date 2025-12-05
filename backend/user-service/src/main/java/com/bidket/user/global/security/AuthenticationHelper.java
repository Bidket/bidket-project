package com.bidket.user.global.security;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * 인증 관련 유틸리티 클래스
 * SecurityContext에서 사용자 정보를 추출하는 공통 로직을 제공합니다.
 */
public class AuthenticationHelper {

    /**
     * SecurityContext에서 현재 사용자 ID 추출
     * 
     * @return 사용자 ID
     * @throws UserException 인증 정보가 없는 경우
     */
    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
        
        try {
            if (authentication.getPrincipal() instanceof UUID) {
                return (UUID) authentication.getPrincipal();
            } else if (authentication.getPrincipal() instanceof String) {
                return UUID.fromString((String) authentication.getPrincipal());
            } else {
                throw new UserException(UserErrorCode.UNAUTHORIZED);
            }
        } catch (IllegalArgumentException e) {
            // UUID 형식이 잘못된 경우
            throw new UserException(UserErrorCode.INVALID_TOKEN);
        } catch (Exception e) {
            throw new UserException(UserErrorCode.UNAUTHORIZED);
        }
    }
}

