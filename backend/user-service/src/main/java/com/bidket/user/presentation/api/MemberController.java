package com.bidket.user.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.user.application.service.BidEligibilityService;
import com.bidket.user.application.service.BlacklistRegisterService;
import com.bidket.user.application.service.BlacklistReleaseService;
import com.bidket.user.application.service.BlacklistStatusService;
import com.bidket.user.application.service.EmailCheckService;
import com.bidket.user.application.service.LoginService;
import com.bidket.user.application.service.NicknameCheckService;
import com.bidket.user.application.service.MyInfoService;
import com.bidket.user.application.service.PermissionsService;
import com.bidket.user.application.service.PointBalanceService;
import com.bidket.user.application.service.PointHistoryService;
import com.bidket.user.application.service.SignupService;
import com.bidket.user.presentation.dto.request.BlacklistRegisterRequest;
import com.bidket.user.presentation.dto.request.LoginRequest;
import com.bidket.user.presentation.dto.request.SignupRequest;
import com.bidket.user.presentation.dto.response.BidEligibilityResponse;
import com.bidket.user.presentation.dto.response.BlacklistRegisterResponse;
import com.bidket.user.presentation.dto.response.BlacklistReleaseResponse;
import com.bidket.user.presentation.dto.response.BlacklistStatusResponse;
import com.bidket.user.presentation.dto.response.EmailCheckResponse;
import com.bidket.user.presentation.dto.response.LoginResponse;
import com.bidket.user.presentation.dto.response.NicknameCheckResponse;
import com.bidket.user.presentation.dto.response.MyInfoResponse;
import com.bidket.user.presentation.dto.response.PermissionsResponse;
import com.bidket.user.presentation.dto.response.PointBalanceResponse;
import com.bidket.user.presentation.dto.response.PointHistoryResponse;
import com.bidket.user.presentation.dto.response.SignupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * 회원 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final SignupService signupService;
    private final LoginService loginService;
    private final MyInfoService myInfoService;
    private final EmailCheckService emailCheckService;
    private final NicknameCheckService nicknameCheckService;
    private final PermissionsService permissionsService;
    private final BlacklistStatusService blacklistStatusService;
    private final BlacklistRegisterService blacklistRegisterService;
    private final BlacklistReleaseService blacklistReleaseService;
    private final BidEligibilityService bidEligibilityService;
    private final PointBalanceService pointBalanceService;
    private final PointHistoryService pointHistoryService;

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
                                    value = "test1@example.com",
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
                                    value = "비드켓",
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
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@RequestBody @Valid SignupRequest request) {
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
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = loginService.login(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("로그인에 성공했습니다.", response));
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
     * 블랙리스트 등록 API
     * @param memberId 블랙리스트로 등록할 회원 ID
     * @param request 블랙리스트 등록 요청 정보 (reason, expireAt)
     * @return 블랙리스트 등록 응답 (memberId, blacklisted, reason, expireAt, createdAt)
     */
    @Operation(summary = "블랙리스트 등록", description = "특정 회원을 블랙리스트로 등록합니다.", tags = {"03. 블랙리스트 관리"}, security = @SecurityRequirement(name = "Bearer Authentication"))
    @PostMapping("/{memberId}/blacklist")
    public ResponseEntity<BlacklistRegisterResponse> registerBlacklist(
            @PathVariable UUID memberId,
            @RequestBody @Valid BlacklistRegisterRequest request) {
        BlacklistRegisterResponse response = blacklistRegisterService.registerBlacklist(memberId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * 블랙리스트 해제 API (관리자)
     * @param memberId 블랙리스트를 해제할 회원 ID
     * @return 블랙리스트 해제 응답 (memberId, blacklisted, updatedAt)
     */
    @Operation(summary = "블랙리스트 해제", description = "특정 회원의 블랙리스트를 해제합니다.", tags = {"03. 블랙리스트 관리"}, security = @SecurityRequirement(name = "Bearer Authentication"))
    @DeleteMapping("/{memberId}/blacklist")
    public ResponseEntity<BlacklistReleaseResponse> releaseBlacklist(
            @PathVariable UUID memberId) {
        BlacklistReleaseResponse response = blacklistReleaseService.releaseBlacklist(memberId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
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
            @RequestParam(required = false) @Schema(example = "0") Integer page,
            @RequestParam(required = false) @Schema(example = "20") Integer size,
            @RequestParam(required = false) @Schema(example = "CHARGE") String type) {
        PointHistoryResponse response = pointHistoryService.getPointHistory(page, size, type);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }
}

