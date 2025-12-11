package com.bidket.auction.global.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class TimezoneConfig {

    @PostConstruct
    public void setDefaultTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }
}


