package com.accommodationfinder.security;

import com.accommodationfinder.dto.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Per-IP limits on the unauthenticated endpoints an attacker would hammer: password
 * guessing on login, mass sign-ups, and anything that sends an email (which costs money
 * and can be used to spam a victim's inbox).
 *
 * Message sending is limited per user inside MessageService instead, because chat also
 * arrives over the WebSocket and never passes through this filter.
 *
 * The client IP is {@code request.getRemoteAddr()}. Behind a reverse proxy that is the
 * proxy's address unless {@code server.forward-headers-strategy} is set - the prod
 * profile sets it, so do not remove it there or every user will share one bucket.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;
    private final Map<String, RateLimitProperties.Rule> rules;

    public RateLimitFilter(RateLimiter rateLimiter, RateLimitProperties properties, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
        this.rules = Map.of(
                "/api/auth/login", properties.login(),
                "/api/auth/register", properties.register(),
                "/api/auth/forgot-password", properties.emailFlows(),
                "/api/auth/reset-password", properties.emailFlows(),
                "/api/auth/verify-email", properties.emailFlows(),
                "/api/auth/resend-verification", properties.emailFlows());
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || !rules.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        RateLimitProperties.Rule rule = rules.get(path);
        String key = path + "|" + request.getRemoteAddr();

        RateLimiter.Decision decision = rateLimiter.tryAcquire(key, rule.limit(), rule.window());
        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        log.info("Rate limit hit on {} from {}", path, request.getRemoteAddr());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError body = ApiError.of(429, HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "Too many attempts - please wait " + decision.retryAfterSeconds() + " seconds and try again",
                path);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
