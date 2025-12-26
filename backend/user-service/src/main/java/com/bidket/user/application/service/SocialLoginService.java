package com.bidket.user.application.service;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.domain.model.PointAccountStatus;
import com.bidket.user.domain.model.Provider;
import com.bidket.user.domain.model.UserRole;
import com.bidket.user.domain.model.UserStatus;
import com.bidket.user.global.security.JwtTokenProvider;
import com.bidket.user.global.security.PasswordEncoder;
import com.bidket.user.infrastructure.external.GoogleTokenVerifier;
import com.bidket.user.infrastructure.persistence.entity.NotificationSetting;
import com.bidket.user.infrastructure.persistence.entity.PointAccount;
import com.bidket.user.infrastructure.persistence.entity.RefreshToken;
import com.bidket.user.infrastructure.persistence.entity.User;
import com.bidket.user.infrastructure.persistence.entity.UserBlacklist;
import com.bidket.user.infrastructure.persistence.repository.NotificationSettingRepository;
import com.bidket.user.infrastructure.persistence.repository.PointAccountRepository;
import com.bidket.user.infrastructure.persistence.repository.RefreshTokenRepository;
import com.bidket.user.infrastructure.persistence.repository.UserBlacklistRepository;
import com.bidket.user.infrastructure.persistence.repository.UserRepository;
import com.bidket.user.presentation.dto.request.SocialLoginRequest;
import com.bidket.user.presentation.dto.response.SocialLoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * 소셜 로그인 서비스
 * 
 * 소셜 로그인 시 다음 작업을 수행:
 * 1. 소셜 서버 인증 및 사용자 식별자 확보 (providerId)
 * 2. provider + providerId로 회원 조회
 * 3. 회원 없으면 자동 가입
 * 4. 블랙리스트/상태 체크
 * 5. JWT 토큰 생성 (accessToken, refreshToken)
 * 6. Refresh Token DB 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialLoginService {

    private final UserRepository userRepository;
    private final UserBlacklistRepository userBlacklistRepository;
    private final PointAccountRepository pointAccountRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * 소셜 로그인 처리
     * 
     * @param request 소셜 로그인 요청 정보
     * @return 소셜 로그인 응답 (JWT 토큰 및 회원 정보)
     * @throws UserException 로그인 실패 시
     */
    @Transactional
    public SocialLoginResponse socialLogin(SocialLoginRequest request) {
        // 1. provider 검증
        Provider provider;
        try {
            provider = Provider.valueOf(request.provider().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new UserException(UserErrorCode.INVALID_REQUEST);
        }

        // 2. provider별 필수 토큰 검증
        if (provider != Provider.GOOGLE) {
            throw new UserException(UserErrorCode.INVALID_REQUEST);
        }
        if (request.idToken() == null || request.idToken().isBlank()) {
            throw new UserException(UserErrorCode.INVALID_REQUEST);
        }

        // 3. 소셜 서버 인증 및 사용자 식별자 확보
        GoogleTokenVerifier.GoogleUserInfo googleUserInfo = googleTokenVerifier.verifyToken(request.idToken());
        String providerId = googleUserInfo.getProviderId();
        String email = googleUserInfo.getEmail();
        String name = googleUserInfo.getName();
        
        // providerId null/blank 방어 (유니크 제약 및 사용자 조회 안정성)
        if (providerId == null || providerId.isBlank()) {
            log.warn("소셜 로그인 시 providerId가 null이거나 비어있음: provider={}", provider);
            throw new UserException(UserErrorCode.INVALID_REQUEST);
        }

        // 4. 회원 조회 (provider + providerId)
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElse(null);

        boolean isNewMember = false;

        // 5. 회원 없으면 자동 가입
        if (user == null) {
            isNewMember = true;
            
            // email이 없으면 null로 저장 (더미 이메일 저장하지 않음)
            String userEmail = (email != null && !email.isBlank()) ? email : null;
            
            // email unique 충돌 체크 (email이 있는 경우에만)
            if (userEmail != null && userRepository.existsByEmail(userEmail)) {
                log.warn("소셜 로그인 자동 가입 시 email 중복 발생: email={}, provider={}, providerId={}", 
                        userEmail, provider, providerId);
                throw new UserException(UserErrorCode.EMAIL_DUPLICATE);
            }
            
            // 소셜 로그인 사용자는 password를 사용하지 않으므로 더미 해시값 저장
            String dummyPassword = passwordEncoder.encode(UUID.randomUUID().toString());
            
            // name이 없을 수 있으므로 기본값 설정
            String userName = name != null && !name.isBlank() ? name : "소셜사용자";

            try {
                user = User.builder()
                        .loginId(null) // 소셜 로그인 사용자는 loginId 없음
                        .provider(provider)
                        .providerId(providerId)
                        .name(userName)
                        .password(dummyPassword)
                        .email(userEmail) // null 허용
                        .nickname(null) // 최초 가입 시 nickname은 NULL
                        .phone(null)
                        .status(UserStatus.ACTIVE)
                        .role(UserRole.ROLE_USER)
                        .build();

                user = userRepository.save(user);
            } catch (DataIntegrityViolationException e) {
                // 동시 요청으로 인한 제약 위반 시 처리 (최종 방어)
                // userEmail != null이고 save에서 제약 위반이면 EMAIL_DUPLICATE로 처리 (안정적/실용적)
                if (userEmail != null) {
                    // 사전에 existsByEmail()로 체크했는데도 동시 요청으로 제약 위반 발생
                    // email이 있으면 EMAIL_DUPLICATE로 처리 (원인 구분보다 안정성이 우선)
                    log.warn("소셜 로그인 자동 가입 시 제약 위반 (email 존재): email={}, provider={}, providerId={}, error={}", 
                            userEmail, provider, providerId, e.getMessage(), e);
                    throw new UserException(UserErrorCode.EMAIL_DUPLICATE);
                }
                // email이 없는 경우는 기타 제약 위반으로 재throw
                throw e;
            }

            // PointAccount 생성
            PointAccount pointAccount = PointAccount.builder()
                    .userId(user.getId())
                    .balance(0L)
                    .status(PointAccountStatus.ACTIVE)
                    .build();
            pointAccountRepository.save(pointAccount);

            // NotificationSetting 생성
            // 최초 자동가입 시에만 marketingAgree 저장
            Boolean marketingAgree = request.marketingAgree() != null ? request.marketingAgree() : false;
            NotificationSetting notificationSetting = NotificationSetting.builder()
                    .userId(user.getId())
                    .allowPush(true)
                    .allowEmail(true)
                    .allowSms(true)
                    .allowMarketing(marketingAgree)
                    .build();
            notificationSettingRepository.save(notificationSetting);
        }

        // 6. 상태 확인 (status = ACTIVE)
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserException(UserErrorCode.INACTIVE_MEMBER);
        }

        // 7. 블랙리스트 확인
        LocalDateTime now = LocalDateTime.now();
        UserBlacklist blacklist = userBlacklistRepository.findActiveBlacklistByUserId(user.getId(), now)
                .orElse(null);

        if (blacklist != null) {
            throw new UserException(UserErrorCode.BLACKLISTED_MEMBER);
        }

        // 8. role 확인 및 기본값 설정 (기존 데이터 대응)
        // role이 null이면 DB에도 저장되도록 업데이트 (@Transactional + 더티 체킹으로 자동 반영)
        if (user.getRole() == null) {
            log.warn("사용자 role이 null입니다. 기본값 ROLE_USER로 설정: userId={}", user.getId());
            user.updateRole(UserRole.ROLE_USER);
        }
        UserRole userRole = user.getRole();

        // 9. JWT 토큰 생성 (accessToken, refreshToken) - role 정보 포함
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), userRole.name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // 10. Refresh Token DB 저장 (userId당 RT 1개 정책, 레이스 컨디션 방지)
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

        // 10. last_login_at 업데이트 (@Transactional + 더티 체킹으로 자동 반영)
        user.updateLastLoginAt();

        // 11. expiresIn 계산 (밀리초를 초로 변환)
        Duration accessTtl = Duration.ofMillis(accessTokenExpiration);
        long expiresInSeconds = accessTtl.toSeconds();

        // 12. 소셜 로그인 응답 생성
        return SocialLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresInSeconds)
                .memberId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .nickname(user.getNickname())
                .provider(provider.name())
                .status(user.getStatus().name())
                .isNewMember(isNewMember)
                .build();
    }
}

