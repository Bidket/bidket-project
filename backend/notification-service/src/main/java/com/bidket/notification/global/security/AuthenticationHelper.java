package com.bidket.notification.global.security;

import com.bidket.notification.domain.exception.NotificationErrorCode;
import com.bidket.notification.domain.exception.NotificationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * 인증 관련 유틸리티 클래스
 */
public class AuthenticationHelper {

    private static final String X_USER_ID_HEADER = "X-User-Id";
    private static final String X_USER_ROLE_HEADER = "X-User-Role";

    /**
     * 현재 사용자 ID 추출
     * 1. Gateway에서 전달한 헤더에서 추출 시도
     * 2. 없으면 SecurityContext에서 추출
     */
    public static UUID getCurrentUserId() {
        // Gateway에서 전달한 헤더에서 user_id 추출 시도
        UUID userIdFromHeader = getUserIdFromHeader();
        if (userIdFromHeader != null) {
            return userIdFromHeader;
        }

        // 헤더에 없으면 SecurityContext에서 추출
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

    /**
     * Gateway에서 전달한 헤더에서 user_id 추출
     */
    private static UUID getUserIdFromHeader() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String userIdHeader = request.getHeader(X_USER_ID_HEADER);
                
                if (userIdHeader != null && !userIdHeader.isEmpty()) {
                    return UUID.fromString(userIdHeader);
                }
            }
        } catch (Exception e) {
            // 헤더 파싱 실패 시 무시하고 다음 방법 시도
        }
        return null;
    }

    /**
     * Gateway에서 전달한 헤더에서 role 추출
     */
    public static String getCurrentUserRole() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String roleHeader = request.getHeader(X_USER_ROLE_HEADER);
                
                if (roleHeader != null && !roleHeader.isEmpty()) {
                    return roleHeader;
                }
            }
        } catch (Exception e) {
            // 헤더 파싱 실패 시 기본값 반환
        }
        return "ROLE_USER"; // 기본값
    }

    /**
     * 현재 사용자가 ADMIN 권한을 가지고 있는지 확인
     */
    public static void requireAdminRole() {
        String role = getCurrentUserRole();
        if (!"ROLE_ADMIN".equals(role)) {
            throw new NotificationException(NotificationErrorCode.FORBIDDEN);
        }
    }
}

