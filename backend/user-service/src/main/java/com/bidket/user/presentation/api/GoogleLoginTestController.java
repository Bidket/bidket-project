package com.bidket.user.presentation.api;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Google 로그인 테스트 페이지 컨트롤러
 * 환경 변수에서 Google Client ID를 주입하여 HTML을 반환합니다.
 */
@RestController
@RequiredArgsConstructor
public class GoogleLoginTestController {

    @Value("${google.client-id}")
    private String googleClientId;

    /**
     * Google 로그인 테스트 페이지 반환
     * HTML 파일의 ${GOOGLE_CLIENT_ID} 플레이스홀더를 실제 환경 변수 값으로 치환
     */
    @GetMapping(value = "/google-login-test.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getGoogleLoginTestPage() throws IOException {
        // HTML 파일 읽기
        ClassPathResource resource = new ClassPathResource("static/google-login-test.html");
        String htmlContent = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        
        // 플레이스홀더를 실제 값으로 치환
        String processedHtml = htmlContent.replace("${GOOGLE_CLIENT_ID}", googleClientId);
        
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(processedHtml);
    }
}

