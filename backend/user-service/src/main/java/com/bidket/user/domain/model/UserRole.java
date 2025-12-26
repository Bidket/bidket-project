package com.bidket.user.domain.model;

/**
 * 사용자 권한 역할
 */
public enum UserRole {
    ROLE_USER("일반 사용자"),
    ROLE_ADMIN("관리자");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

