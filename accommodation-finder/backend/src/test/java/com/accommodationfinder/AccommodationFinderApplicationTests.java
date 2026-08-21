package com.accommodationfinder;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Fails fast if any bean, config property or JPA mapping is broken. */
@SpringBootTest
@ActiveProfiles("test")
class AccommodationFinderApplicationTests {

    @Test
    void contextLoads() {
        // The assertion is the successful startup itself.
    }
}
