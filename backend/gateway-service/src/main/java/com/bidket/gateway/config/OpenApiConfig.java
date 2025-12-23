package com.bidket.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        String jwtSchemeName = "Bearer Authentication";
        return new OpenAPI()
                .info(new Info()
                        .title("Bidket Gateway API Dashboard")
                        .description(
                                "### 신발 입찰/경매 대기열 처리 플랫폼 \n"
                                + "----------------------------------------- \n"
                                + "Bidket 서비스의 모든 마이크로서비스 API를 통합 제공하는 게이트웨이 대시보드입니다.<br>"
                                + "우측 상단의 **[Select a definition]** 드롭박스를 클릭하여 각 마이크로서비스의 상세 API 명세를 확인하실 수 있습니다."
                        )
                        .version("v1")
                        .contact(new Contact()
                                .name("🔗 Team Bidket Github")
                                .url("https://github.com/Bidket/bidket-project")))

                // 모든 API에 보안 요구사항 추가
                .addSecurityItem(new SecurityRequirement().addList(jwtSchemeName))

                // JWT 보안 스키마 정의
                .components(new Components()
                        .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                                .name(jwtSchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("발급받은 JWT 토큰을 입력하세요. (Bearer는 자동으로 붙습니다)")));
    }
}
