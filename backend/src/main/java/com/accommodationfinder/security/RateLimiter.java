package com.accommodationfinder.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window request counter, keyed by whatever the caller chooses (an IP, a user id).
 *
 * In memory on purpose: one instance needs no Redis, and a limit that resets when the
 * server restarts is still a limit. Running several instances behind a load balancer
 * multiplies every limit by the instance count - move the counters to Redis (or use
 * nginx {@code limit_req}) at that point.
 */
@Component
public class RateLimiter {

    /** Outcome of one attempt. {@code retryAfterSeconds} is only meaningful when refused. */
    public record Decision(boolean allowed, long retryAfterSeconds) {
    }

    private record Window(long startMillis, int count) {
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;
    private final boolean enabled;

    @Autowired
    public RateLimiter(@Value("${app.rate-limit.enabled:true}") boolean enabled) {
        this(enabled, Clock.systemUTC());
    }

    RateLimiter(boolean enabled, Clock clock) {
        this.enabled = enabled;
        this.clock = clock;
    }

    /** Counts one attempt against {@code key} and says whether it is within {@code limit} per {@code window}. */
    public Decision tryAcquire(String key, int limit, Duration window) {
        if (!enabled) {
            return new Decision(true, 0);
        }
        long now = clock.millis();
        long windowMillis = window.toMillis();
        Window updated = windows.compute(key, (k, current) ->
                current == null || now - current.startMillis() >= windowMillis
                        ? new Window(now, 1)
                        : new Window(current.startMillis(), current.count() + 1));

        if (updated.count() <= limit) {
            return new Decision(true, 0);
        }
        long retryAfterMillis = updated.startMillis() + windowMillis - now;
        return new Decision(false, Math.max(1, (retryAfterMillis + 999) / 1000));
    }

    /** Drops windows nobody has touched for an hour, so the map cannot grow without bound. */
    @Scheduled(fixedDelay = 10 * 60 * 1000)
    void evictStale() {
        long cutoff = clock.millis() - Duration.ofHours(1).toMillis();
        windows.values().removeIf(window -> window.startMillis() < cutoff);
    }
}
