package com.bidket.notification.global.security;

import com.bidket.notification.domain.exception.NotificationErrorCode;
import com.bidket.notification.domain.exception.NotificationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * 인증 관련 유틸리티 클래스
 */
public class AuthenticationHelper {

    /**
     * SecurityContext에서 현재 사용자 ID 추출
     */
    public static UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new NotificationException(NotificationErrorCode.UNAUTHORIZED);
        }
        
        try {
            if (authentication.getPrincipal() instanceof UUID) {
                return (UUID) authentication.getPrincipal();
            } else if (authentication.getPrincipal() instanceof String) {
                return UUID.fromString((String) authentication.getPrincipal());
            } else {
                throw new NotificationException(NotificationErrorCode.UNAUTHORIZED);
            }
        } catch (IllegalArgumentException e) {
            throw new NotificationException(NotificationErrorCode.INVALID_TOKEN);
        } catch (Exception e) {
            throw new NotificationException(NotificationErrorCode.UNAUTHORIZED);
        }
    }
}

