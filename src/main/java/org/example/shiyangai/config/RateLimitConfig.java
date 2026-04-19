package org.example.shiyangai.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    /**
     * 配置限流器：每秒最多5个请求
     */
    @Bean
    public RateLimiter rateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(5)           // 每个周期最多5个请求
                .limitRefreshPeriod(Duration.ofSeconds(1))  // 周期1秒
                .timeoutDuration(Duration.ofSeconds(2))     // 等待超时2秒
                .build();

        return RateLimiter.of("api-limiter", config);
    }
}