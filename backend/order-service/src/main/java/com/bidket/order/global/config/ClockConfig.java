package com.bidket.order.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Clock 설정
 *
 * 시간 관련 로직의 테스트 가능성을 높이기 위해 Clock Bean을 제공합니다.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
