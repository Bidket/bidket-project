package com.bidket.user.application.service;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.domain.model.Provider;
import com.bidket.user.domain.model.UserStatus;
import com.bidket.user.global.security.JwtTokenProvider;
import com.bidket.user.global.security.PasswordEncoder;
import com.bidket.user.infrastructure.persistence.entity.User;
import com.bidket.user.infrastructure.persistence.entity.UserBlacklist;
import com.bidket.user.infrastructure.persistence.repository.UserBlacklistRepository;
import com.bidket.user.infrastructure.persistence.repository.UserRepository;
import com.bidket.user.presentation.dto.request.LoginRequest;
import com.bidket.user.presentation.dto.response.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 로그인 서비스
 * 
 * 로그인 시 다음 작업을 수행:
 * 1. loginId로 사용자 조회 (provider = LOCAL)
 * 2. 비밀번호 검증 (BCrypt)
 * 3. 상태 확인 (status = ACTIVE)
 * 4. 블랙리스트 확인 (p_user_blacklist에서 active=true & (expire_at IS NULL OR expire_at > now()))
 * 5. JWT 토큰 생성 (accessToken, refreshToken)
 * 6. last_login_at 업데이트
 */
@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepository userRepository;
    private final UserBlacklistRepository userBlacklistRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

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

        // 5. JWT 토큰 생성 (accessToken, refreshToken)
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // 6. last_login_at 업데이트
        user.updateLastLoginAt();
        userRepository.save(user);

        // 7. expiresIn 계산 (밀리초를 초로 변환)
        long expiresInSeconds = accessTokenExpiration / 1000;

        // 8. 로그인 응답 생성
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
                .role("ROLE_USER") // 기본 권한 (추후 User-Role 관계 구현 시 수정 필요)
                .status(user.getStatus().name())
                .build();
    }
}

