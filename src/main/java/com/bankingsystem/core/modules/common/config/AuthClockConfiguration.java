package com.bankingsystem.core.modules.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class AuthClockConfiguration {

    @Bean
    public Clock authClock() {
        return Clock.systemUTC();
    }
}
