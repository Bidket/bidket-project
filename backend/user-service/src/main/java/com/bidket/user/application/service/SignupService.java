package com.bidket.user.application.service;

import com.bidket.user.domain.exception.UserErrorCode;
import com.bidket.user.domain.exception.UserException;
import com.bidket.user.domain.model.PointAccountStatus;
import com.bidket.user.domain.model.Provider;
import com.bidket.user.domain.model.UserStatus;
import com.bidket.user.global.security.JwtTokenProvider;
import com.bidket.user.global.security.PasswordEncoder;
import com.bidket.user.global.security.PasswordValidator;
import com.bidket.user.infrastructure.persistence.entity.NotificationSetting;
import com.bidket.user.infrastructure.persistence.entity.PointAccount;
import com.bidket.user.infrastructure.persistence.entity.User;
import com.bidket.user.infrastructure.persistence.repository.NotificationSettingRepository;
import com.bidket.user.infrastructure.persistence.repository.PointAccountRepository;
import com.bidket.user.infrastructure.persistence.repository.UserRepository;
import com.bidket.user.presentation.dto.request.SignupRequest;
import com.bidket.user.presentation.dto.response.SignupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원가입 서비스
 * 
 * 회원가입 시 다음 작업을 수행
 * 1. 이용 약관 동의 검증
 * 2. 이메일/로그인 아이디/닉네임 중복 확인
 * 3. 비밀번호 강도 검증
 * 4. 비밀번호 BCrypt 암호화
 * 5. User 엔티티 생성 (provider = LOCAL, status = ACTIVE)
 * 6. PointAccount 생성 (balance = 0, status = ACTIVE)
 * 7. NotificationSetting 생성 (기본값 설정)
 * 8. JWT 토큰 생성 (accessToken, refreshToken)
 */
@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserRepository userRepository;
    private final PointAccountRepository pointAccountRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원가입 처리
     * 
     * @param request 회원가입 요청 정보
     * @return 회원가입 응답 (회원 정보 + JWT 토큰)
     * @throws UserException 이용 약관 미동의, 이메일/로그인 아이디/닉네임 중복 시
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        // 이용 약관 동의 검증 (DB 컬럼은 없고 서버에서 검증만 수행)
        if (!request.termsAgreement()) {
            throw new UserException(UserErrorCode.TERMS_NOT_AGREED);
        }

        // 이메일 중복 확인
        if (userRepository.existsByEmail(request.email())) {
            throw new UserException(UserErrorCode.EMAIL_DUPLICATE);
        }

        // 로그인 아이디 중복 확인
        if (userRepository.existsByLoginId(request.loginId())) {
            throw new UserException(UserErrorCode.LOGIN_ID_DUPLICATE);
        }

        // 닉네임 중복 확인 (닉네임이 제공된 경우에만)
        if (request.nickname() != null && !request.nickname().isBlank()
                && userRepository.existsByNickname(request.nickname())) {
            throw new UserException(UserErrorCode.NICKNAME_DUPLICATE);
        }

        // 비밀번호 강도 검증
        if (!passwordValidator.isValid(request.password())) {
            throw new UserException(UserErrorCode.WEAK_PASSWORD);
        }

        // 비밀번호 BCrypt 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // User 엔티티 생성
        // provider = LOCAL, provider_id = NULL, status = ACTIVE
        User user = User.builder()
                .loginId(request.loginId())
                .provider(Provider.LOCAL)
                .providerId(null)
                .name(request.name())
                .password(encodedPassword)
                .email(request.email())
                .nickname(request.nickname())
                .phone(request.phone())
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);

        // PointAccount 생성 (member_id로 잔액 0, status = ACTIVE 행 생성)
        PointAccount pointAccount = PointAccount.builder()
                .userId(savedUser.getId())
                .balance(0L)
                .status(PointAccountStatus.ACTIVE)
                .build();
        pointAccountRepository.save(pointAccount);

        // NotificationSetting 생성
        // allow_push/email/sms 기본값 true, allow_marketing = marketingAgree
        NotificationSetting notificationSetting = NotificationSetting.builder()
                .userId(savedUser.getId())
                .allowPush(true)
                .allowEmail(true)
                .allowSms(true)
                .allowMarketing(request.marketingAgree())
                .build();
        notificationSettingRepository.save(notificationSetting);

        // JWT 토큰 생성 (accessToken, refreshToken)
        String accessToken = jwtTokenProvider.generateAccessToken(savedUser.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getId());

        // 회원가입 응답 생성
        return SignupResponse.builder()
                .memberId(savedUser.getId())
                .loginId(savedUser.getLoginId())
                .email(savedUser.getEmail())
                .nickname(savedUser.getNickname())
                .createdAt(savedUser.getCreatedAt())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
