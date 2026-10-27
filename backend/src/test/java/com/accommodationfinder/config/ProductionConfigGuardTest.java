package com.accommodationfinder.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionConfigGuardTest {

    private static final String REAL_SECRET = "a-real-secret-generated-with-openssl-rand-base64-48";

    @Test
    void acceptsASafeConfiguration() {
        assertThatCode(() -> new ProductionConfigGuard(REAL_SECRET, false, false, "smtp.example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void refusesThePublicDemoSecret() {
        assertThatThrownBy(() -> new ProductionConfigGuard(
                "demo-only-insecure-jwt-secret-do-not-use-in-production", false, false, "smtp.example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("demo secret");
    }

    @Test
    void refusesTheH2ConsoleAndDemoSeeding() {
        assertThatThrownBy(() -> new ProductionConfigGuard(REAL_SECRET, true, true, "smtp.example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("h2.console")
                .hasMessageContaining("seed");
    }
}
