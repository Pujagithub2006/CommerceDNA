package io.commercedna.api.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Rate Limiting Configuration for CommerceDNA API.
 * Prevents abuse and ensures fair usage across all endpoints.
 */
@Configuration
public class RateLimitConfig {

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(100) // 100 requests per period
                .limitRefreshPeriod(Duration.ofSeconds(1)) // Refresh every second
                .timeoutDuration(Duration.ofSeconds(5)) // Wait max 5 seconds for permit
                .build();

        return RateLimiterRegistry.of(config);
    }

    @Bean
    public RateLimiter apiRateLimiter(RateLimiterRegistry registry) {
        return registry.rateLimiter("apiRateLimiter");
    }

    @Bean
    public RateLimiter webhookRateLimiter(RateLimiterRegistry registry) {
        // More lenient rate limiting for webhooks
        RateLimiterConfig webhookConfig = RateLimiterConfig.custom()
                .limitForPeriod(50) // 50 webhook events per second
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofSeconds(2))
                .build();
        
        return registry.rateLimiter("webhookRateLimiter", webhookConfig);
    }

    @Bean
    public RateLimiter negotiationRateLimiter(RateLimiterRegistry registry) {
        // Stricter rate limiting for AI negotiation endpoints
        RateLimiterConfig negotiationConfig = RateLimiterConfig.custom()
                .limitForPeriod(20) // 20 negotiation requests per second
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .timeoutDuration(Duration.ofSeconds(3))
                .build();
        
        return registry.rateLimiter("negotiationRateLimiter", negotiationConfig);
    }
}