package com.accommodationfinder.config;

import com.accommodationfinder.model.Role;
import com.accommodationfinder.model.User;
import com.accommodationfinder.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first administrator on a fresh production database, where seeding is
 * off and there is otherwise no way to get an admin without editing rows by hand.
 *
 * Runs only when ADMIN_EMAIL and ADMIN_PASSWORD are both set, and only while no admin
 * exists - so it is a one-shot: once the account is there, the variables can (and
 * should) be removed from the environment. It runs after DataSeeder, so in the demo
 * profile it never stops the demo data from being loaded.
 */
@Component
@Order(3)
public class AdminBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    static final int MIN_PASSWORD_LENGTH = 12;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String name;

    public AdminBootstrap(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.bootstrap.admin-email:}") String email,
                          @Value("${app.bootstrap.admin-password:}") String password,
                          @Value("${app.bootstrap.admin-name:Administrator}") String name) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email == null ? "" : email.trim();
        this.password = password == null ? "" : password;
        this.name = name;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (email.isEmpty() && password.isEmpty()) {
            return;                                   // not configured - the normal case
        }
        if (email.isEmpty() || password.isEmpty()) {
            throw new IllegalStateException("Set both ADMIN_EMAIL and ADMIN_PASSWORD, or neither");
        }
        if (userRepository.countByRole(Role.ADMIN) > 0) {
            log.info("An admin account already exists - ADMIN_EMAIL/ADMIN_PASSWORD ignored; "
                    + "you can remove them from the environment");
            return;
        }
        if (!email.contains("@")) {
            throw new IllegalStateException("ADMIN_EMAIL is not an email address");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            // Never promote an existing account: whoever registered that address first
            // chose its password, so promoting it could hand admin to a stranger.
            throw new IllegalStateException("ADMIN_EMAIL belongs to an existing account - "
                    + "choose an address that has not been registered");
        }

        User admin = new User(name, email.toLowerCase(), passwordEncoder.encode(password), Role.ADMIN);
        userRepository.save(admin);
        log.warn("Bootstrap admin account created for {} - remove ADMIN_PASSWORD from the environment now",
                admin.getEmail());
    }
}
