package com.accommodationfinder.config;

import com.accommodationfinder.model.Role;
import com.accommodationfinder.model.User;
import com.accommodationfinder.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    private static final String STRONG_PASSWORD = "correct-horse-battery";

    @Mock private UserRepository userRepository;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private AdminBootstrap bootstrap(String email, String password) {
        return new AdminBootstrap(userRepository, encoder, email, password, "Ops");
    }

    @Test
    void doesNothingWhenNotConfigured() {
        bootstrap("", "").run();
        verifyNoInteractions(userRepository);
    }

    @Test
    void createsTheFirstAdminWithAHashedPassword() {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(userRepository.existsByEmailIgnoreCase("Ops@Example.com")).thenReturn(false);

        bootstrap("Ops@Example.com", STRONG_PASSWORD).run();

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.getValue().getEmail()).isEqualTo("ops@example.com");
        assertThat(saved.getValue().getPassword()).isNotEqualTo(STRONG_PASSWORD);
        assertThat(encoder.matches(STRONG_PASSWORD, saved.getValue().getPassword())).isTrue();
    }

    @Test
    void isAOneShotOnceAnAdminExists() {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        bootstrap("ops@example.com", STRONG_PASSWORD).run();

        verify(userRepository, never()).save(any());
    }

    @Test
    void neverPromotesAnAccountSomebodyElseRegistered() {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
        when(userRepository.existsByEmailIgnoreCase("ops@example.com")).thenReturn(true);

        assertThatThrownBy(() -> bootstrap("ops@example.com", STRONG_PASSWORD).run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("existing account");
        verify(userRepository, never()).save(any());
    }

    @Test
    void refusesAWeakPassword() {
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);

        assertThatThrownBy(() -> bootstrap("ops@example.com", "short").run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 12");
    }

    @Test
    void refusesHalfAConfiguration() {
        assertThatThrownBy(() -> bootstrap("ops@example.com", "").run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("both");
    }
}
