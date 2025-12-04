package com.bidket.user.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.user.application.service.BlacklistRegisterService;
import com.bidket.user.application.service.BlacklistStatusService;
import com.bidket.user.application.service.EmailCheckService;
import com.bidket.user.application.service.LoginService;
import com.bidket.user.application.service.MyInfoService;
import com.bidket.user.application.service.PermissionsService;
import com.bidket.user.application.service.SignupService;
import com.bidket.user.presentation.dto.request.BlacklistRegisterRequest;
import com.bidket.user.presentation.dto.request.LoginRequest;
import com.bidket.user.presentation.dto.request.SignupRequest;
import com.bidket.user.presentation.dto.response.BlacklistRegisterResponse;
import com.bidket.user.presentation.dto.response.BlacklistStatusResponse;
import com.bidket.user.presentation.dto.response.EmailCheckResponse;
import com.bidket.user.presentation.dto.response.LoginResponse;
import com.bidket.user.presentation.dto.response.MyInfoResponse;
import com.bidket.user.presentation.dto.response.PermissionsResponse;
import com.bidket.user.presentation.dto.response.SignupResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final PermissionsService permissionsService;
    private final BlacklistStatusService blacklistStatusService;
    private final BlacklistRegisterService blacklistRegisterService;

    /**
     * 회원가입 API
     * 
     * @param request 회원가입 요청 정보 (loginId, password, email, nickname, name, phone, marketingAgree, termsAgreement)
     * @return 회원가입 응답 (memberId, loginId, email, nickname, createdAt, accessToken, refreshToken)
     */
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
     * 
     * 아이디 + 비밀번호 로그인 → 블랙리스트(p_user_blacklist) active인 유저는 로그인 차단
     * 
     * @param request 로그인 요청 정보 (loginId, password)
     * @return 로그인 응답 (accessToken, refreshToken, tokenType, expiresIn, memberId, loginId, nickname, name, email, role, status)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = loginService.login(request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("로그인에 성공했습니다.", response));
    }

    /**
     * 이메일 중복 체크 API
     * 
     * 비로그인/로그인 모두 사용 가능 (회원 가입 화면 등)
     * 회원가입 화면에서 실시간 중복 체크용으로 사용함
     * 
     * @param email 중복 여부를 확인할 이메일 주소 (Query Parameter)
     * @return 이메일 중복 체크 응답 (email, available, reason)
     */
    @GetMapping("/check-email")
    public ResponseEntity<EmailCheckResponse> checkEmail(
            @RequestParam(required = false) String email) {
        EmailCheckResponse response = emailCheckService.checkEmail(email);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    /**
     * 권한 조회 API
     * 관리자가 특정 회원의 권한/역할 정보(예: ROLE_USER, ROLE_ADMIN) 조회
     * Authorization 헤더에 Bearer JWT 토큰이 필요
     * @param memberId 조회할 회원 ID
     * @return 권한 조회 응답 (memberId, roles)
     */
    @GetMapping("/{memberId}/permissions")
    public ResponseEntity<PermissionsResponse> getPermissions(@PathVariable UUID memberId) {
        PermissionsResponse response = permissionsService.getPermissions(memberId);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    /**
     * 블랙리스트 상태 조회 API
     * 관리자가 특정 회원의 블랙리스트 여부를 조회
     * 입찰 가능 여부 판단 등에 활용
     * Authorization 헤더에 Bearer JWT 토큰이 필요
     * @param memberId 조회할 회원 ID
     * @return 블랙리스트 상태 조회 응답 (memberId, isBlacklisted, reason, expireAt)
     */
    @GetMapping("/{memberId}/blacklist-status")
    public ResponseEntity<BlacklistStatusResponse> getBlacklistStatus(@PathVariable UUID memberId) {
        BlacklistStatusResponse response = blacklistStatusService.getBlacklistStatus(memberId);
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
     * 내 정보 조회 API
     * 
     * 현재 로그인한 사용자의 정보를 조회
     * Authorization 헤더에 Bearer JWT 토큰이 필요
     * 
     * @return 내 정보 응답 (memberId, loginId, email, nickname, role, createdAt, status)
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyInfoResponse>> getMyInfo() {
        MyInfoResponse response = myInfoService.getMyInfo();
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success("내 정보 조회에 성공했습니다.", response));
    }
}

