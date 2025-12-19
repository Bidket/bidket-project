package com.bidket.user.presentation.dto.response;

import lombok.Builder;

import java.util.UUID;

/**
 * 사용자 이메일 조회 응답 DTO
 * @param userId 회원 ID (UUID)
 * @param email 이메일 주소
 */
@Builder
public record UserEmailResponse(
        UUID userId,
        String email
) {
}

