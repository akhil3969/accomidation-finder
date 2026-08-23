package com.accommodationfinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the Accommodation Finder platform.
 *
 * <p>Profiles:
 * <ul>
 *   <li><b>demo</b> - in-memory H2 database, auto-seeded (default, zero setup)</li>
 *   <li><b>dev</b>  - local MySQL 8</li>
 *   <li><b>prod</b> - externally configured datasource, schema validated only</li>
 * </ul>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AccommodationFinderApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccommodationFinderApplication.class, args);
    }
}
