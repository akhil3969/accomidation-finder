package com.accommodationfinder.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Refuses to start the prod profile with settings that are only safe on a laptop.
 * A misconfigured deploy should fail loudly at boot, not run insecurely for months.
 */
@Component
@Profile("prod")
public class ProductionConfigGuard {

    public ProductionConfigGuard(@Value("${app.jwt.secret:}") String jwtSecret,
                                 @Value("${spring.h2.console.enabled:false}") boolean h2ConsoleEnabled,
                                 @Value("${app.seed.enabled:false}") boolean seedEnabled) {
        List<String> problems = new ArrayList<>();
        if (jwtSecret.startsWith("demo-only-")) {
            problems.add("app.jwt.secret is the public demo secret - set JWT_SECRET");
        }
        if (h2ConsoleEnabled) {
            problems.add("spring.h2.console.enabled must be false");
        }
        if (seedEnabled) {
            problems.add("app.seed.enabled must be false - demo accounts use a public password");
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Unsafe production configuration: " + String.join("; ", problems));
        }
    }
}
