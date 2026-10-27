package com.accommodationfinder;

import com.accommodationfinder.dto.BookingDtos.BookingRequest;
import com.accommodationfinder.model.BookingStatus;
import com.accommodationfinder.model.Role;
import com.accommodationfinder.model.User;
import com.accommodationfinder.repository.RoomRepository;
import com.accommodationfinder.repository.UserRepository;
import com.accommodationfinder.security.JwtService;
import com.accommodationfinder.service.BookingService;
import com.accommodationfinder.support.TestFixtures;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The migrations against a real MySQL 8, with Hibernate in {@code validate} mode - exactly
 * what the prod profile does. If an entity and the migrations ever disagree, this fails
 * in CI rather than at the first production deploy.
 *
 * Every other test uses H2, whose dialect differs from MySQL in ways that hide bugs, so
 * this also runs a booking through the real database (including the row lock).
 *
 * Needs Docker. Skipped, not failed, on a machine without it; GitHub Actions has it.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver"
})
@ActiveProfiles("test")
class MySqlMigrationIT {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @Autowired private Flyway flyway;
    @Autowired private BookingService bookingService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    @Test
    void everyMigrationAppliesAndTheEntitiesMatchTheResult() {
        // Reaching this line means Hibernate's validate pass accepted the migrated schema.
        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied).isNotEmpty();
        assertThat(applied).allSatisfy(m -> assertThat(m.getState().isFailed()).isFalse());
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void aBookingRoundTripsThroughRealMySql() {
        TestFixtures fixtures = new TestFixtures(userRepository, roomRepository, passwordEncoder, jwtService);
        User landlord = fixtures.user(Role.LANDLORD, true);
        User tenant = fixtures.user(Role.TENANT, true);
        Long roomId = fixtures.room(landlord).getId();

        LocalDate checkIn = LocalDate.now().plusDays(14);
        var booking = bookingService.request(new BookingRequest(roomId, checkIn, checkIn.plusDays(30), 1, "Hi"), tenant);

        assertThat(booking.status()).isEqualTo(BookingStatus.PENDING);
    }
}
