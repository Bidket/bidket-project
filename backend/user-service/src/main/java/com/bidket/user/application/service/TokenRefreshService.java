package com.bidket.user.application.service;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.global.security.JwtTokenProvider;
import com.bidket.user.infrastructure.persistence.entity.RefreshToken;
import com.bidket.user.infrastructure.persistence.entity.User;
import com.bidket.user.infrastructure.persistence.repository.RefreshTokenRepository;
import com.bidket.user.infrastructure.persistence.repository.UserRepository;
import com.bidket.user.presentation.dto.request.TokenRefreshRequest;
import com.bidket.user.presentation.dto.response.TokenRefreshResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 토큰 재발급 서비스
 * 1. Refresh Token JWT 서명 검증
 * 2. Refresh Token에서 userId 추출
 * 3. DB에서 Refresh Token 존재 여부 확인 (삭제 정책: DB에 없으면 무효화됨)
 * 4. RT-userId 불일치 검증 (보안: DB에 저장된 userId와 JWT의 userId 일치 확인)
 * 5. Refresh Token 만료 여부 확인
 * 6. 기존 Refresh Token 삭제 (회전: userId당 RT 1개 유지)
 * 7. 새로운 Access Token, Refresh Token 생성
 * 8. 새 Refresh Token DB 저장
 */
@Service
@RequiredArgsConstructor
public class TokenRefreshService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * 토큰 재발급 처리
     * @param request 토큰 재발급 요청 정보 (refreshToken)
     * @return 토큰 재발급 응답 (accessToken, refreshToken, tokenType, expiresIn)
     * @throws UserException Refresh Token이 유효하지 않은 경우 (만료, 위조, DB에 없음)
     */
    @Transactional
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        String refreshTokenValue = request.refreshToken();

        // 1. Refresh Token JWT 서명 검증
        if (!jwtTokenProvider.validateToken(refreshTokenValue)) {
            throw new UserException(UserErrorCode.INVALID_TOKEN);
        }

        // 2. Refresh Token에서 userId 추출
        UUID userId;
        try {
            userId = jwtTokenProvider.getUserIdFromToken(refreshTokenValue);
        } catch (Exception e) {
            throw new UserException(UserErrorCode.INVALID_TOKEN);
        }

        // 3. DB에서 Refresh Token 존재 여부 확인 (삭제 정책: DB에 없으면 무효화됨)
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new UserException(UserErrorCode.INVALID_TOKEN));

        // 4. RT-userId 불일치 검증 (보안: DB에 저장된 userId와 JWT의 userId 일치 확인)
        if (!refreshTokenEntity.getUserId().equals(userId)) {
            throw new UserException(UserErrorCode.INVALID_TOKEN);
        }

        // 5. Refresh Token 만료 여부 확인
        LocalDateTime now = LocalDateTime.now();
        if (refreshTokenEntity.isExpired(now)) {
            // 만료된 토큰은 삭제
            refreshTokenRepository.delete(refreshTokenEntity);
            throw new UserException(UserErrorCode.INVALID_TOKEN);
        }

        // 6. 기존 Refresh Token 삭제 (회전: userId당 RT 1개 유지)
        refreshTokenRepository.deleteByUserId(userId);

        // 7. 사용자 조회 (role 정보를 위해 필요)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        // 8. 새로운 Access Token, Refresh Token 생성 (role 정보 포함)
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, user.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId);

        // 9. 새 Refresh Token DB 저장
        Duration refreshTtl = Duration.ofMillis(refreshTokenExpiration);
        LocalDateTime newRefreshTokenExpiresAt = now.plus(refreshTtl);
        RefreshToken newRefreshTokenEntity = RefreshToken.builder()
                .userId(userId)
                .token(newRefreshToken)
                .expiresAt(newRefreshTokenExpiresAt)
                .build();
        refreshTokenRepository.save(newRefreshTokenEntity);

        // 10. expiresIn 계산 (밀리초를 초로 변환)
        Duration accessTtl = Duration.ofMillis(accessTokenExpiration);
        long expiresInSeconds = accessTtl.toSeconds();

        // 11. 토큰 재발급 응답 생성
        return new TokenRefreshResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                expiresInSeconds
        );
    }
}

