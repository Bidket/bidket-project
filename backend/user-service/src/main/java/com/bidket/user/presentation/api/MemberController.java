package com.bidket.user.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.user.application.service.LoginService;
import com.bidket.user.application.service.MyInfoService;
import com.bidket.user.application.service.SignupService;
import com.bidket.user.presentation.dto.request.LoginRequest;
import com.bidket.user.presentation.dto.request.SignupRequest;
import com.bidket.user.presentation.dto.response.LoginResponse;
import com.bidket.user.presentation.dto.response.MyInfoResponse;
import com.bidket.user.presentation.dto.response.SignupResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

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

