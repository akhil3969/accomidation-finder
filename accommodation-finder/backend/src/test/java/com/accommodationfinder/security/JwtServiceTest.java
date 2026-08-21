package com.accommodationfinder.security;

import com.accommodationfinder.model.Role;
import com.accommodationfinder.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "unit-test-secret-key-that-is-definitely-long-enough", 3_600_000L, 7_200_000L);
        jwtService = new JwtService(properties);

        user = new User("Akhil", "akhil@epita.fr", "hash", Role.TENANT);
        user.setId(42L);
    }

    @Test
    void generatesATokenThatRoundTripsTheSubjectAndUserId() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("akhil@epita.fr");
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtService.isTokenValid(token, "akhil@epita.fr")).isTrue();
    }

    @Test
    void rejectsATokenIssuedForSomebodyElse() {
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenValid(token, "someone@else.fr")).isFalse();
    }

    @Test
    void rejectsGarbage() {
        assertThat(jwtService.isTokenValid("not-a-jwt", "akhil@epita.fr")).isFalse();
    }

    @Test
    void refusesToStartWithAShortSecret() {
        JwtProperties weak = new JwtProperties("too-short", 1000L, 2000L);
        assertThatThrownBy(() -> new JwtService(weak))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 characters");
    }
}
