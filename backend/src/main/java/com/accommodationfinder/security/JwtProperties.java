package com.accommodationfinder.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds the {@code app.jwt.*} block from application.yml. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long expirationMs,
        long refreshExpirationMs
) {
}
