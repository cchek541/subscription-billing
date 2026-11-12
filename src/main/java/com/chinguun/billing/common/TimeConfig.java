package com.chinguun.billing.common;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {

    // Billing dates follow Korean time, so a plan bought on Jan 31 at 23:30 KST renews on Feb 28 KST.
    public static final ZoneId BILLING_ZONE = ZoneId.of("Asia/Seoul");

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
