package com.bidket.user.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) 설정
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "User Service API",
                description = "회원 관리 서비스 API 문서",
                version = "1.0.0"
        ),
        tags = {
                @Tag(name = "01. 회원 인증", description = "회원가입, 로그인, 이메일 체크 API"),
                @Tag(name = "02. 회원 정보", description = "권한 조회, 입찰 가능 여부 체크 API"),
                @Tag(name = "03. 블랙리스트 관리", description = "블랙리스트 상태 조회 및 등록 API"),
                @Tag(name = "04. 포인트", description = "포인트 잔액 및 거래 내역 조회 API")
        }
)
@SecurityScheme(
        name = "Bearer Authentication",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class OpenApiConfig {
}

