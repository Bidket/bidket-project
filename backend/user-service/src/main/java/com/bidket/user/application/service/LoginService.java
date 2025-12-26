package com.bidket.user.application.service;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.domain.model.Provider;
import com.bidket.user.domain.model.UserRole;
import com.bidket.user.domain.model.UserStatus;
import com.bidket.user.global.security.JwtTokenProvider;
import com.bidket.user.global.security.PasswordEncoder;
import com.bidket.user.infrastructure.persistence.entity.RefreshToken;
import com.bidket.user.infrastructure.persistence.entity.User;
import com.bidket.user.infrastructure.persistence.entity.UserBlacklist;
import com.bidket.user.infrastructure.persistence.repository.RefreshTokenRepository;
import com.bidket.user.infrastructure.persistence.repository.UserBlacklistRepository;
import com.bidket.user.infrastructure.persistence.repository.UserRepository;
import com.bidket.user.presentation.dto.request.LoginRequest;
import com.bidket.user.presentation.dto.response.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 로그인 서비스
 * 로그인 시 다음 작업을 수행:
 * 1. loginId로 사용자 조회 (provider = LOCAL)
 * 2. 비밀번호 검증 (BCrypt)
 * 3. 상태 확인 (status = ACTIVE)
 * 4. 블랙리스트 확인 (p_user_blacklist에서 active=true & (expire_at IS NULL OR expire_at > now()))
 * 5. JWT 토큰 생성 (accessToken, refreshToken)
 * 6. last_login_at 업데이트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepository userRepository;
    private final UserBlacklistRepository userBlacklistRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * 로그인 처리
     * 
     * @param request 로그인 요청 정보 (loginId, password)
     * @return 로그인 응답 (JWT 토큰 및 회원 정보)
     * @throws UserException 로그인 실패 시 (INVALID_CREDENTIALS, INACTIVE_MEMBER, BLACKLISTED_MEMBER)
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 1. loginId로 사용자 조회 (provider = LOCAL)
        User user = userRepository.findByLoginId(request.loginId())
                .orElseThrow(() -> new UserException(UserErrorCode.INVALID_CREDENTIALS));

        // LOCAL provider 확인
        if (user.getProvider() != Provider.LOCAL) {
            throw new UserException(UserErrorCode.INVALID_CREDENTIALS);
        }

        // 2. 비밀번호 검증 (BCrypt)
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UserException(UserErrorCode.INVALID_CREDENTIALS);
        }

        // 3. 상태 확인 (status = ACTIVE)
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserException(UserErrorCode.INACTIVE_MEMBER);
        }

        // 4. 블랙리스트 확인
        // p_user_blacklist에서 user_id 기준으로 active=true & (expire_at IS NULL OR expire_at > now()) 존재 여부 확인
        LocalDateTime now = LocalDateTime.now();
        UserBlacklist blacklist = userBlacklistRepository.findActiveBlacklistByUserId(user.getId(), now)
                .orElse(null);

        if (blacklist != null) {
            throw new UserException(UserErrorCode.BLACKLISTED_MEMBER);
        }

        // 5. role 확인 및 기본값 설정 (기존 데이터 대응)
        // role이 null이면 DB에도 저장되도록 업데이트 (@Transactional + 더티 체킹으로 자동 반영)
        if (user.getRole() == null) {
            log.warn("사용자 role이 null입니다. 기본값 ROLE_USER로 설정: userId={}", user.getId());
            user.updateRole(UserRole.ROLE_USER);
        }
        UserRole userRole = user.getRole();

        // 6. JWT 토큰 생성 (accessToken, refreshToken) - role 정보 포함
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), userRole.name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // 7. Refresh Token DB 저장 (userId당 RT 1개 정책, 레이스 컨디션 방지)
        Duration refreshTtl = Duration.ofMillis(refreshTokenExpiration);
        LocalDateTime refreshTokenExpiresAt = now.plus(refreshTtl);
        
        // user_id에 UNIQUE 제약이 있으므로, 기존 토큰이 있으면 업데이트, 없으면 생성
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUserId(user.getId());
        RefreshToken refreshTokenEntity;
        
        if (existingToken.isPresent()) {
            // 기존 토큰 업데이트
            refreshTokenEntity = existingToken.get();
            refreshTokenEntity.updateToken(refreshToken, refreshTokenExpiresAt);
            refreshTokenRepository.save(refreshTokenEntity);
        } else {
            // 새 토큰 생성 (UNIQUE 제약 위반 시 재시도)
            try {
                refreshTokenEntity = RefreshToken.builder()
                        .userId(user.getId())
                        .token(refreshToken)
                        .expiresAt(refreshTokenExpiresAt)
                        .build();
                refreshTokenRepository.save(refreshTokenEntity);
            } catch (DataIntegrityViolationException e) {
                // 동시 요청으로 인한 충돌 시, 기존 토큰을 조회하여 업데이트
                log.debug("RefreshToken 저장 중 UNIQUE 제약 위반 발생, 기존 토큰 업데이트로 전환: userId={}", user.getId());
                refreshTokenEntity = refreshTokenRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new UserException(UserErrorCode.SERVER_ERROR));
                refreshTokenEntity.updateToken(refreshToken, refreshTokenExpiresAt);
                refreshTokenRepository.save(refreshTokenEntity);
            }
        }

        // 8. last_login_at 업데이트
        user.updateLastLoginAt();
        // @Transactional + JPA 더티 체킹으로 자동 업데이트됨 (save() 불필요)

        // 9. expiresIn 계산 (밀리초를 초로 변환)
        Duration accessTtl = Duration.ofMillis(accessTokenExpiration);
        long expiresInSeconds = accessTtl.toSeconds();

        // 10. 로그인 응답 생성
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresInSeconds)
                .memberId(user.getId())
                .loginId(user.getLoginId())
                .nickname(user.getNickname())
                .name(user.getName())
                .email(user.getEmail())
                .role(userRole.name())
                .status(user.getStatus().name())
                .build();
    }
}

