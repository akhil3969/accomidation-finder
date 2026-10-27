package com.accommodationfinder.service;

import com.accommodationfinder.exception.BadRequestException;
import com.accommodationfinder.exception.ConflictException;
import com.accommodationfinder.model.Role;
import com.accommodationfinder.model.User;
import com.accommodationfinder.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private UserRepository userRepository;
    @Spy private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    @InjectMocks private AccountService accountService;

    private User admin;

    @BeforeEach
    void setUp() {
        admin = new User("Only Admin", "admin@test.fr", passwordEncoder.encode("correct-password"), Role.ADMIN);
        admin.setId(7L);
    }

    @Test
    void theLastActiveAdminCannotDeleteThemselves() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndEnabledTrue(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> accountService.deleteOwnAccount(admin, "correct-password"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("only administrator");
        verify(userRepository, never()).save(any());
    }

    @Test
    void aWrongPasswordStopsDeletionBeforeAnythingIsTouched() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> accountService.deleteOwnAccount(admin, "wrong"))
                .isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).save(any());
    }
}
