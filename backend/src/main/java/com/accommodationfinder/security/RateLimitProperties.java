package com.accommodationfinder.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Binds {@code app.rate-limit.*}. Each rule is "at most {@code limit} attempts per {@code window}". */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        Rule login,
        Rule register,
        Rule emailFlows,
        Rule messages
) {
    public record Rule(int limit, Duration window) {
    }
}
