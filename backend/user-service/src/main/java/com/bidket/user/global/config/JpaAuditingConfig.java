package com.bidket.user.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정
 * 
 * BaseEntity의 @CreatedDate, @LastModifiedDate가 자동으로 설정되도록 합니다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}

