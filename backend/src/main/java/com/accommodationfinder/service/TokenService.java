package com.accommodationfinder.service;

import com.accommodationfinder.exception.BadRequestException;
import com.accommodationfinder.model.TokenType;
import com.accommodationfinder.model.User;
import com.accommodationfinder.model.UserToken;
import com.accommodationfinder.repository.UserTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/** Issues and redeems the single-use tokens behind email links. */
@Service
public class TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserTokenRepository tokenRepository;

    public TokenService(UserTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    /** Returns the raw token for the email. Only its hash is stored; earlier tokens of the same type stop working. */
    @Transactional
    public String issue(User user, TokenType type, Duration validFor) {
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.retireActive(user.getId(), type, now);

        byte[] bytes = new byte[32];                     // 256 bits - not guessable
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        tokenRepository.save(new UserToken(user, type, hash(raw), now.plus(validFor)));
        return raw;
    }

    /** Marks the token used and returns its owner, or fails with one message for every way it can be wrong. */
    @Transactional
    public User consume(String raw, TokenType type) {
        if (raw == null || raw.isBlank()) {
            throw invalid();
        }
        LocalDateTime now = LocalDateTime.now();
        UserToken token = tokenRepository.findByHashAndType(hash(raw.trim()), type)
                .filter(t -> t.isUsable(now))
                .orElseThrow(TokenService::invalid);
        token.setUsedAt(now);
        return token.getUser();
    }

    static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is missing from this JVM", ex);
        }
    }

    // Unknown, expired and already-used all read the same, so the response leaks nothing.
    private static BadRequestException invalid() {
        return new BadRequestException("This link is invalid or has expired - request a new one");
    }
}
