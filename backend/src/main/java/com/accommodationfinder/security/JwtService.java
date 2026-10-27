package com.accommodationfinder.security;

import com.accommodationfinder.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Issues and validates the stateless HS256 access tokens used by the API and the WebSocket handshake. */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /**
     * Secrets that were once committed to the public repository. Anyone can sign
     * tokens with them, so they are refused in every profile.
     */
    static final String TOKEN_VERSION_CLAIM = "tv";

    static final Set<String> LEAKED_SECRETS = Set.of(
            "change-me-in-production-accommodation-finder-secret-key-2026");

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = buildKey(properties.secret());
    }

    private static SecretKey buildKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret must be set (see backend/.env.example)");
        }
        if (LEAKED_SECRETS.contains(secret.trim())) {
            throw new IllegalStateException(
                    "app.jwt.secret is a value that was published in the repository - "
                            + "generate a new one with: openssl rand -base64 48");
        }

        byte[] keyBytes;
        try {
            // Support both a base64 secret and a plain passphrase.
            keyBytes = Decoders.BASE64.decode(secret);
            if (keyBytes.length < 32) {
                keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            }
        } catch (RuntimeException ex) {
            // Not valid base64 - jjwt raises its own DecodingException here, which is a
            // JwtException rather than an IllegalArgumentException. Catching the narrower
            // type let it escape the constructor and killed bean creation at startup.
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least 32 characters (256 bits) for HS256");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("name", user.getName());
        claims.put(TOKEN_VERSION_CLAIM, user.getTokenVersion());

        Date now = new Date();
        Date expiry = new Date(now.getTime() + properties.expirationMs());

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public long getExpirationMs() {
        return properties.expirationMs();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Object uid = parseClaims(token).get("uid");
        return uid == null ? null : Long.valueOf(uid.toString());
    }

    public boolean isTokenValid(String token, String expectedUsername) {
        try {
            Claims claims = parseClaims(token);
            return claims.getSubject().equalsIgnoreCase(expectedUsername)
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected JWT: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Full check against the account behind the token: signature, expiry, subject, the
     * account still enabled, and the token's version matching the account's. JWTs are
     * stateless, so that last comparison is what makes "reset password" actually sign out
     * a thief who already holds a token.
     */
    public boolean isTokenValidFor(String token, User user) {
        try {
            Claims claims = parseClaims(token);
            Object version = claims.get(TOKEN_VERSION_CLAIM);
            // Tokens minted before versions existed carry no claim; they count as version 0.
            int tokenVersion = version == null ? 0 : Integer.parseInt(version.toString());
            return claims.getSubject().equalsIgnoreCase(user.getEmail())
                    && claims.getExpiration().after(new Date())
                    && user.isEnabled()
                    && tokenVersion == user.getTokenVersion();
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected JWT: {}", ex.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
