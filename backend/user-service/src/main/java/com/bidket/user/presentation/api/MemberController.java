package com.bidket.user.presentation.api;

import com.bidket.common.presentation.response.ApiResponse;
import com.bidket.user.application.service.SignupService;
import com.bidket.user.presentation.dto.request.SignupRequest;
import com.bidket.user.presentation.dto.response.SignupResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}

