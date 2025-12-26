package com.bidket.user.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.user.application.service.BidEligibilityService;
import com.bidket.user.application.service.BlacklistListService;
import com.bidket.user.application.service.BlacklistRegisterService;
import com.bidket.user.application.service.BlacklistReleaseService;
import com.bidket.user.application.service.BlacklistStatusService;
import com.bidket.user.application.service.EmailCheckService;
import com.bidket.user.application.service.LoginService;
import com.bidket.user.application.service.LogoutService;
import com.bidket.user.application.service.NicknameCheckService;
import com.bidket.user.application.service.TokenRefreshService;
import com.bidket.user.application.service.MyInfoService;
import com.bidket.user.application.service.PermissionsService;
import com.bidket.user.application.service.PointBalanceService;
import com.bidket.user.application.service.PointHistoryService;
import com.bidket.user.application.service.MemberDeactivationService;
import com.bidket.user.application.service.PasswordChangeService;
import com.bidket.user.application.service.ProfileUpdateService;
import com.bidket.user.application.service.SignupService;
import com.bidket.user.application.service.SocialLoginService;
import com.bidket.user.application.service.HistoryService;
import com.bidket.user.global.security.AuthorizationTokenValidator;
import com.bidket.user.presentation.dto.request.BlacklistRegisterRequest;
import com.bidket.user.presentation.dto.request.LoginRequest;
import com.bidket.user.presentation.dto.request.MemberDeactivationRequest;
import com.bidket.user.presentation.dto.request.SocialLoginRequest;
import com.bidket.user.presentation.dto.request.TokenRefreshRequest;
import com.bidket.user.presentation.dto.request.PasswordChangeRequest;
import com.bidket.user.presentation.dto.request.ProfileUpdateRequest;
import com.bidket.user.presentation.dto.request.SignupRequest;
import com.bidket.user.presentation.dto.response.BidEligibilityResponse;
import com.bidket.user.presentation.dto.response.BlacklistListResponse;
import com.bidket.user.presentation.dto.response.BlacklistRegisterResponse;
import com.bidket.user.presentation.dto.response.BlacklistReleaseResponse;
import com.bidket.user.presentation.dto.response.BlacklistStatusResponse;
import com.bidket.user.presentation.dto.response.EmailCheckResponse;
import com.bidket.user.presentation.dto.response.LoginResponse;
import com.bidket.user.presentation.dto.response.LogoutResponse;
import com.bidket.user.presentation.dto.response.NicknameCheckResponse;
import com.bidket.user.presentation.dto.response.SocialLoginResponse;
import com.bidket.user.presentation.dto.response.TokenRefreshResponse;
import com.bidket.user.presentation.dto.response.MyInfoResponse;
import com.bidket.user.presentation.dto.response.PermissionsResponse;
import com.bidket.user.presentation.dto.response.PointBalanceResponse;
import com.bidket.user.presentation.dto.response.MemberDeactivationResponse;
import com.bidket.user.presentation.dto.response.PasswordChangeResponse;
import com.bidket.user.presentation.dto.response.PointHistoryResponse;
import com.bidket.user.presentation.dto.response.ProfileUpdateResponse;
import com.bidket.user.presentation.dto.response.SignupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.UUID;

/**
 * 회원 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/v1/members")
@RequiredArgsConstructor
@Validated
public class MemberController {

    private final SignupService signupService;
    private final LoginService loginService;
    private final SocialLoginService socialLoginService;
    private final LogoutService logoutService;
    private final TokenRefreshService tokenRefreshService;
    private final MyInfoService myInfoService;
    private final EmailCheckService emailCheckService;
    private final NicknameCheckService nicknameCheckService;
    private final PermissionsService permissionsService;
    private final BlacklistStatusService blacklistStatusService;
    private final BlacklistListService blacklistListService;
    private final BlacklistRegisterService blacklistRegisterService;
    private final BlacklistReleaseService blacklistReleaseService;
    private final BidEligibilityService bidEligibilityService;
    private final PointBalanceService pointBalanceService;
    private final PointHistoryService pointHistoryService;
    private final ProfileUpdateService profileUpdateService;
    private final PasswordChangeService passwordChangeService;
    private final MemberDeactivationService memberDeactivationService;
    private final HistoryService historyService;

    /**
     * 이메일 중복 체크 API
     * 비로그인/로그인 모두 사용 가능 (회원 가입 화면 등)
     * 회원가입 화면에서 실시간 중복 체크용으로 사용함
     * @param email 중복 여부를 확인할 이메일 주소 (Query Parameter)
     * @return 이메일 중복 체크 응답 (email, available, reason)
     */
    @Operation(summary = "이메일 중복 체크", description = "이메일 주소의 중복 여부를 확인합니다.", tags = {"01. 회원 인증"}, operationId = "auth-01-check-email")
    @GetMapping("/check-email")
    public ResponseEntity<EmailCheckResponse> checkEmail(
            @Parameter(
                    description = "중복 여부를 확인할 이메일 주소",
                    examples = {
                            @ExampleObject(
                                    name = "사용 가능한 이메일",
                                    summary = "사용 가능",
                                    value = "newuser@example.com",
                                    description = "등록되지 않은 이메일로 사용 가능"
                            ),
                            @ExampleObject(
                                    name = "중복된 이메일",
                                    summary = "중복됨",
                                    value = "testuser@example.com",
                                    description = "이미 등록된 이메일로 사용 불가"
                            )
                    }
            )
            @RequestParam(required = false) String email) {
        EmailCheckResponse response = emailCheckService.checkEmail(email);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    /**
     * 닉네임 중복 체크 API
     * @param nickname 중복 여부를 확인할 닉네임 (Query Parameter)
     * @return 닉네임 중복 체크 응답 (nickname, available, reason)
     */
    @Operation(summary = "닉네임 중복 체크", description = "닉네임의 중복 여부를 확인합니다.", tags = {"01. 회원 인증"}, operationId = "auth-01-check-nickname")
    @GetMapping("/check-nickname")
    public ResponseEntity<NicknameCheckResponse> checkNickname(
            @Parameter(
                    description = "중복 여부를 확인할 닉네임",
                    examples = {
                            @ExampleObject(
                                    name = "사용 가능한 닉네임",
                                    summary = "사용 가능",
                                    value = "새로운닉네임",
                                    description = "등록되지 않은 닉네임으로 사용 가능"
                            ),
                            @ExampleObject(
                                    name = "중복된 닉네임",
                                    summary = "중복됨",
                                    value = "테스트유저",
                                    description = "이미 등록된 닉네임으로 사용 불가"
                            )
                    }
            )
            @RequestParam(required = false) String nickname) {
        NicknameCheckResponse response = nicknameCheckService.checkNickname(nickname);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    /**
     * 회원가입 API
     * @param request 회원가입 요청 정보 (loginId, password, email, nickname, name, phone, marketingAgree, termsAgreement)
     * @return 회원가입 응답 (memberId, loginId, email, nickname, createdAt, accessToken, refreshToken)
     */
    @Operation(summary = "회원가입", description = "새로운 회원을 등록합니다.", tags = {"01. 회원 인증"}, operationId = "auth-02-signup")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = SignupResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "유효하지 않은 요청"
            )
    })
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@RequestBody(required = true) @Valid SignupRequest request) {
        SignupResponse response = signupService.signup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .location(URI.create("/v1/members/" + response.memberId()))
                .body(ApiResponse.success("회원가입이 완료되었습니다.", response));
    }

    /**
     * 로그인 API
     * 아이디 + 비밀번호 로그인 → 블랙리스트(p_user_blacklist) active인 유저는 로그인 차단
     * @param request 로그인 요청 정보 (loginId, password)
     * @return 로그인 응답 (accessToken, refreshToken, tokenType, expiresIn, memberId, loginId, nickname, name, email, role, status)
     */
    @Operation(summary = "로그인", description = "아이디와 비밀번호로 로그인합니다.", tags = {"01. 회원 인증"}, operationId = "auth-03-login")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            )
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody(required = true) @Valid LoginRequest request) {
        LoginResponse response = loginService.login(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("로그인에 성공했습니다.", response));
    }

    /**
     * 소셜 로그인 API
     * POST /v1/members/social-login
     * 구글 소셜 로그인 (회원가입/로그인 겸용)
     * @param request 소셜 로그인 요청 정보 (provider=GOOGLE, idToken, deviceId, marketingAgree)
     * @return 소셜 로그인 응답 (accessToken, refreshToken, tokenType, expiresIn, memberId, email, name, nickname, provider, status, isNewMember)
     */
    @Operation(
            summary = "소셜 로그인",
            description = "구글 소셜 로그인을 수행합니다. 최초 로그인 시 자동으로 회원가입이 진행됩니다.\n\n" +
                    "idToken은 Google 로그인 후 발급받은 토큰을 사용하세요. " +
                    "테스트를 위해서는 /google-login-test.html 페이지를 이용하거나 Google Identity Services를 통해 획득할 수 있습니다.",
            tags = {"01. 회원 인증"},
            operationId = "auth-04-social-login"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "소셜 로그인 성공",
                    content = @Content(schema = @Schema(implementation = SocialLoginResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (provider 또는 인증 토큰 누락)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "소셜 인증 토큰이 유효하지 않음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "블랙리스트 회원 또는 비활성화된 계정"
            )
    })
    @PostMapping("/social-login")
    public ResponseEntity<ApiResponse<SocialLoginResponse>> socialLogin(
            @RequestBody(required = true) @Valid SocialLoginRequest request) {
        SocialLoginResponse response = socialLoginService.socialLogin(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("소셜 로그인에 성공했습니다.", response));
    }

    /**
     * 토큰 재발급 API
     * POST /v1/members/token/refresh
     * @param request 토큰 재발급 요청 정보 (refreshToken)
     * @return 토큰 재발급 응답 (accessToken, refreshToken, tokenType, expiresIn)
     */
    @Operation(
            summary = "토큰 재발급",
            description = "유효한 Refresh Token을 사용하여 새로운 Access Token과 Refresh Token을 발급받습니다. 재발급 성공 시 Refresh Token은 회전되며, 기존 RT는 즉시 무효화됩니다. 인증/권한 불필요하며, 유효한 Refresh Token만으로 재발급 가능합니다.",
            tags = {"01. 회원 인증"}
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "토큰 재발급 성공",
                    content = @Content(
                            schema = @Schema(implementation = TokenRefreshResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "refreshToken 누락/빈값"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "refreshToken 만료/위조/서명 불일치/DB에 없음(무효화됨)"
            )
    })
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(
            @RequestBody @Valid TokenRefreshRequest request) {
        TokenRefreshResponse response = tokenRefreshService.refreshToken(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("토큰 재발급에 성공했습니다.", response));
    }

    /**
     * 로그아웃 API
     * POST /v1/members/logout
     * 현재 로그인 세션을 종료하고 Refresh Token을 무효화
     * 멱등성 보장: 이미 로그아웃된 상태여도 동일하게 성공 응답을 반환
     * @return 로그아웃 응답 (success)
     */
    @Operation(
            summary = "로그아웃",
            description = "현재 로그인 세션을 종료하고 Refresh Token을 무효화합니다. Authorization 헤더(Bearer JWT 토큰)가 필수이며, 이를 통해 사용자를 식별하여 해당 사용자의 모든 Refresh Token을 무효화합니다. 멱등성 보장: 이미 로그아웃된 상태여도 성공 응답을 반환합니다.",
            tags = {"01. 회원 인증"},
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 처리 완료(또는 이미 로그아웃 상태 포함)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Authorization 누락 또는 유효하지 않은 토큰"
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout() {
        LogoutResponse response = logoutService.logout();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("로그아웃이 완료되었습니다.", response));
    }

    /**
     * 내 정보 조회 API
     * 현재 로그인한 사용자의 정보를 조회
     * Authorization 헤더에 Bearer JWT 토큰이 필요
     * @return 내 정보 응답 (memberId, loginId, email, nickname, role, createdAt, status)
     */
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.", tags = {"01. 회원 인증"}, operationId = "auth-04-get-my-info", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MyInfoResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            )
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyInfoResponse>> getMyInfo() {
        MyInfoResponse response = myInfoService.getMyInfo();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("내 정보 조회에 성공했습니다.", response));
    }

    /**
     * 프로필 수정 API
     * @param request 프로필 수정 요청 정보 (nickname, phone, email 중 최소 1개 필수)
     * @return 프로필 수정 응답 (userId, nickname, phone, email, updatedAt)
     */
    @Operation(
            summary = "프로필 수정",
            description = "현재 로그인한 사용자의 프로필 정보를 수정합니다. 최소 1개 필드는 포함되어야 합니다.",
            tags = {"02. 회원 정보"},
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = ProfileUpdateResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (최소 1개 필드 미포함, 형식 오류 등)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "중복된 이메일 또는 닉네임"
            )
    })
    @PatchMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileUpdateResponse>> updateProfile(
            @RequestBody(required = true) @Valid ProfileUpdateRequest request) {
        ProfileUpdateResponse response = profileUpdateService.updateProfile(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("프로필 수정에 성공했습니다.", response));
    }

    /**
     * 비밀번호 변경 API
     * @param request 비밀번호 변경 요청 정보 (currentPassword, newPassword)
     * @return 비밀번호 변경 응답 (success, changedAt)
     */
    @Operation(
            summary = "비밀번호 변경",
            description = "현재 로그인한 사용자의 비밀번호를 변경합니다.",
            tags = {"02. 회원 정보"},
            security = @SecurityRequirement(name = "Bearer Authentication")
            
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "비밀번호 변경 성공",
                    content = @Content(schema = @Schema(implementation = PasswordChangeResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (현재 비밀번호 불일치, 비밀번호 강도 부족, 새 비밀번호가 현재 비밀번호와 동일 등)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요 (토큰 누락/만료/위조 등)"
            )
    })
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<PasswordChangeResponse>> changePassword(
            @RequestBody(required = true) @Valid PasswordChangeRequest request) {
        PasswordChangeResponse response = passwordChangeService.changePassword(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("비밀번호 변경에 성공했습니다.", response));
    }

    /**
     * 회원 탈퇴(비활성화) API
     * DELETE /v1/members/me
     * @param request 회원 탈퇴 요청 정보 (reason - 선택사항)
     * @return 회원 탈퇴 응답 (memberId, status, deactivatedAt)
     */
    @Operation(
            summary = "회원 탈퇴(비활성화)",
            description = "현재 로그인한 사용자의 계정을 비활성화 처리합니다. 논리삭제 방식으로 데이터는 유지되며, 탈퇴 후 모든 기능이 제한됩니다.",
            tags = {"02. 회원 정보"},
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "회원 탈퇴 성공",
                    content = @Content(schema = @Schema(implementation = MemberDeactivationResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요 (토큰 누락/만료/위조 등)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 탈퇴한 사용자"
            )
    })
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<MemberDeactivationResponse>> deactivateMember(
            @RequestBody(required = false) MemberDeactivationRequest request) {
        // request가 null인 경우 빈 객체로 처리
        MemberDeactivationRequest deactivationRequest = request != null 
                ? request 
                : new MemberDeactivationRequest(null);
        
        MemberDeactivationResponse response = memberDeactivationService.deactivateMember(deactivationRequest);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("회원 탈퇴가 완료되었습니다.", response));
    }

    /**
     * 권한 조회 API
     * 현재 로그인한 회원의 권한/역할 정보(예: ROLE_USER, ROLE_ADMIN) 조회
     * Authorization 헤더에 Bearer JWT 토큰이 필요
     * @return 권한 조회 응답 (memberId, roles)
     */
    @Operation(summary = "권한 조회", description = "현재 로그인한 회원의 권한/역할 정보를 조회합니다.", tags = {"02. 회원 정보"}, security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/permissions")
    public ResponseEntity<PermissionsResponse> getPermissions() {
        PermissionsResponse response = permissionsService.getPermissions();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    /**
     * 블랙리스트 상태 조회 API
     * 현재 로그인한 회원의 블랙리스트 여부를 조회
     * 입찰 가능 여부 판단 등에 활용
     * Authorization 헤더에 Bearer JWT 토큰이 필요
     * @return 블랙리스트 상태 조회 응답 (memberId, isBlacklisted, reason, expireAt)
     */
    @Operation(summary = "블랙리스트 상태 조회", description = "현재 로그인한 회원의 블랙리스트 여부를 조회합니다.", tags = {"03. 블랙리스트 관리"}, security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/blacklist-status")
    public ResponseEntity<BlacklistStatusResponse> getBlacklistStatus() {
        BlacklistStatusResponse response = blacklistStatusService.getBlacklistStatus();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    /**
     * 블랙리스트 목록 조회 API (관리자)
     * @param page 페이지 번호 (0부터 시작, 기본값 0)
     * @param size 페이지 사이즈 (기본값 20)
     * @param activeOnly 현재 유효한 블랙리스트만 조회할지 여부 (기본값 true)
     * @return 블랙리스트 목록 조회 응답 (페이지네이션 포함)
     */
    @Operation(summary = "블랙리스트 목록 조회", description = "블랙리스트 목록을 조회합니다.", tags = {"03. 블랙리스트 관리"}, security = @SecurityRequirement(name = "Bearer Authentication"))
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/blacklist")
    public ResponseEntity<ApiResponse<BlacklistListResponse>> getBlacklistList(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(required = false, defaultValue = "0") @Min(0) Integer page,
            @Parameter(description = "페이지 사이즈", example = "20")
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) Integer size,
            @Parameter(description = "현재 유효한 블랙리스트만 조회할지 여부", example = "true")
            @RequestParam(required = false, defaultValue = "true") Boolean activeOnly) {
        BlacklistListResponse response = blacklistListService.getBlacklistList(page, size, activeOnly);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("블랙리스트 목록 조회에 성공했습니다.", response));
    }

    /**
     * 블랙리스트 등록 API
     * @param memberId 블랙리스트로 등록할 회원 ID
     * @param request 블랙리스트 등록 요청 정보 (reason, expireAt)
     * @return 블랙리스트 등록 응답 (memberId, blacklisted, reason, expireAt, createdAt)
     */
    @Operation(summary = "블랙리스트 등록", description = "특정 회원을 블랙리스트로 등록합니다.", tags = {"03. 블랙리스트 관리"}, security = @SecurityRequirement(name = "Bearer Authentication"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{memberId}/blacklist")
    public ResponseEntity<ApiResponse<BlacklistRegisterResponse>> registerBlacklist(
            @PathVariable UUID memberId,
            @RequestBody @Valid BlacklistRegisterRequest request) {
        BlacklistRegisterResponse response = blacklistRegisterService.registerBlacklist(memberId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("블랙리스트 등록에 성공했습니다.", response));
    }

    /**
     * 블랙리스트 해제 API (관리자)
     * @param memberId 블랙리스트를 해제할 회원 ID
     * @return 블랙리스트 해제 응답 (memberId, blacklisted, updatedAt)
     */
    @Operation(summary = "블랙리스트 해제", description = "특정 회원의 블랙리스트를 해제합니다.", tags = {"03. 블랙리스트 관리"}, security = @SecurityRequirement(name = "Bearer Authentication"))
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{memberId}/blacklist")
    public ResponseEntity<ApiResponse<BlacklistReleaseResponse>> releaseBlacklist(
            @PathVariable UUID memberId) {
        BlacklistReleaseResponse response = blacklistReleaseService.releaseBlacklist(memberId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("블랙리스트 해제에 성공했습니다.", response));
    }

    /**
     * 입찰 가능 여부 체크 API
     * 현재 로그인한 회원의 입찰 가능 여부를 확인
     * 블랙리스트 여부, 회원상태 확인
     * Authorization 헤더에 Bearer JWT 토큰이 필요
     * @return 입찰 가능 여부 체크 응답 (eligible, reasons, memberStatus)
     */
    @Operation(summary = "입찰 가능 여부 체크", description = "현재 로그인한 회원의 입찰 가능 여부를 확인합니다.", tags = {"02. 회원 정보"}, security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/bid-eligibility")
    public ResponseEntity<BidEligibilityResponse> checkBidEligibility() {
        BidEligibilityResponse response = bidEligibilityService.checkBidEligibility();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    /**
     * 포인트 잔액 조회 API
     * 현재 로그인한 사용자의 포인트 잔액을 조회
     * Authorization 헤더에 Bearer JWT 토큰이 필요
     * @return 포인트 잔액 조회 응답 (memberId, balance, currency, updatedAt)
     */
    @Operation(summary = "포인트 잔액 조회", description = "현재 로그인한 사용자의 포인트 잔액을 조회합니다.", tags = {"04. 포인트"}, security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/points")
    public ResponseEntity<PointBalanceResponse> getPointBalance() {
        PointBalanceResponse response = pointBalanceService.getPointBalance();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    /**
     * 포인트 히스토리 조회 API
     * 현재 로그인한 사용자의 포인트 거래 내역을 조회
     * Authorization 헤더에 Bearer JWT 토큰이 필요
     * @param page 페이지 번호 (0부터 시작, 기본값 0)
     * @param size 페이지 사이즈 (기본값 20)
     * @param type 필터용 타입 (CHARGE, USE, REFUND, CANCEL 등, 선택사항)
     * @return 포인트 히스토리 조회 응답 (페이지네이션 포함)
     */
    @Operation(summary = "포인트 히스토리 조회", description = "현재 로그인한 사용자의 포인트 거래 내역을 조회합니다.", tags = {"04. 포인트"}, security = @SecurityRequirement(name = "Bearer Authentication"))
    @GetMapping("/points/history")
    public ResponseEntity<PointHistoryResponse> getPointHistory(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0", schema = @Schema(defaultValue = "0"))
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 사이즈", example = "20", schema = @Schema(defaultValue = "20"))
            @RequestParam(required = false) Integer size,
            @Parameter(description = "필터용 타입 (CHARGE, USE, REFUND, CANCEL 등)", example = "CHARGE")
            @RequestParam(required = false) String type) {
        PointHistoryResponse response = pointHistoryService.getPointHistory(page, size, type);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    /**
     * 입찰/주문 내역 조회 API
     * type=BID인 경우: Auction 서비스의 "내 입찰 내역" API를 호출
     * type=ORDER인 경우: Order 서비스의 "내 주문/결제 내역" API를 호출
     */
    @Operation(
            summary = "입찰/주문 내역 조회",
            description = "현재 로그인한 사용자의 활동 내역을 조회합니다. type=BID인 경우 Auction 서비스를 호출하고, type=ORDER인 경우 Order 서비스를 호출합니다.",
            tags = {"02. 회원 정보"}
    )
    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0", schema = @Schema(defaultValue = "0"))
            @RequestParam(required = false) Integer page,
            @Parameter(description = "페이지 사이즈", example = "20", schema = @Schema(defaultValue = "20"))
            @RequestParam(required = false) Integer size,
            @Parameter(description = "히스토리 타입 (BID 또는 ORDER)", example = "BID", schema = @Schema(defaultValue = "BID"))
            @RequestParam(required = false) String type,
            HttpServletRequest request) {
        
        // Authorization 헤더 추출 (HttpServletRequest에서 자동으로 가져옴)
        String authorization = request.getHeader("Authorization");
        
        // Authorization 헤더 검증 및 정규화 (Bearer 형식 보장)
        // 검증과 전송에 동일한 정규화된 값을 사용하여 일관성 보장
        String normalizedAuthorization = AuthorizationTokenValidator.validateAndNormalize(authorization);
        
        // 기본값 설정
        String historyType = (type != null && !type.isEmpty()) ? type : "BID";
        
        // 타입에 따라 다른 응답 반환
        Object response = historyService.getHistory(page, size, type, normalizedAuthorization);
        
        if ("ORDER".equalsIgnoreCase(historyType)) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("주문 내역 조회에 성공했습니다.", response));
        } else {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(ApiResponse.success("입찰 내역 조회에 성공했습니다.", response));
        }
    }
}

