package com.accommodationfinder.service;

import com.accommodationfinder.dto.AuthDtos.AuthResponse;
import com.accommodationfinder.dto.AuthDtos.LoginRequest;
import com.accommodationfinder.dto.AuthDtos.RegisterRequest;
import com.accommodationfinder.dto.AuthDtos.ResetPasswordRequest;
import com.accommodationfinder.dto.UserDtos.UserSummary;
import com.accommodationfinder.exception.ConflictException;
import com.accommodationfinder.exception.ForbiddenException;
import com.accommodationfinder.model.Role;
import com.accommodationfinder.model.TokenType;
import com.accommodationfinder.model.User;
import com.accommodationfinder.repository.UserRepository;
import com.accommodationfinder.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    static final Duration VERIFICATION_VALIDITY = Duration.ofHours(48);
    static final Duration RESET_VALIDITY = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final String termsVersion;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       TokenService tokenService,
                       EmailService emailService,
                       @Value("${app.legal.terms-version}") String termsVersion) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.termsVersion = termsVersion;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account already exists for " + email);
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        // ADMIN is never self-service; anything else defaults to TENANT.
        user.setRole(request.role() == Role.LANDLORD ? Role.LANDLORD : Role.TENANT);
        // Validation already refused anything but true; this records when and to what.
        user.setTermsAcceptedAt(LocalDateTime.now());
        user.setTermsVersion(termsVersion);

        User saved = userRepository.save(user);
        emailService.sendVerification(saved, tokenService.issue(saved, TokenType.EMAIL_VERIFICATION, VERIFICATION_VALIDITY));

        String token = jwtService.generateToken(saved);
        return AuthResponse.of(token, jwtService.getExpirationMs(), UserSummary.from(saved));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (DisabledException ex) {
            throw new BadCredentialsException("This account has been disabled");
        } catch (AuthenticationException ex) {
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        String token = jwtService.generateToken(user);
        return AuthResponse.of(token, jwtService.getExpirationMs(), UserSummary.from(user));
    }

    // ------------------------------------------------------------ email verification

    @Transactional
    public UserSummary verifyEmail(String rawToken) {
        User user = tokenService.consume(rawToken, TokenType.EMAIL_VERIFICATION);
        user.setEmailVerified(true);
        return UserSummary.from(userRepository.save(user));
    }

    @Transactional
    public void resendVerification(User current) {
        User user = userRepository.findById(current.getId()).orElseThrow();
        if (user.isEmailVerified()) {
            throw new ConflictException("Your email address is already confirmed");
        }
        emailService.sendVerification(user, tokenService.issue(user, TokenType.EMAIL_VERIFICATION, VERIFICATION_VALIDITY));
    }

    // ---------------------------------------------------------------- password reset

    /**
     * Always succeeds from the caller's point of view. Answering "no such account" would
     * let anyone test which email addresses are registered here.
     */
    @Transactional
    public void forgotPassword(String rawEmail) {
        String email = rawEmail.trim().toLowerCase();
        userRepository.findByEmailIgnoreCase(email)
                .filter(User::isEnabled)
                .ifPresentOrElse(
                        user -> emailService.sendPasswordReset(user,
                                tokenService.issue(user, TokenType.PASSWORD_RESET, RESET_VALIDITY)),
                        () -> log.debug("Password reset requested for an unknown or disabled address"));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = tokenService.consume(request.token(), TokenType.PASSWORD_RESET);
        if (!user.isEnabled()) {
            throw new ForbiddenException("This account has been disabled");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        // Every token issued before now stops working: a reset after a break-in has to
        // throw the intruder out, not just change the lock for next time.
        user.revokeAllTokens();
        // Receiving the reset link proves the inbox belongs to them.
        user.setEmailVerified(true);
        userRepository.save(user);
        emailService.sendPasswordChanged(user);
    }

    /** Thrown by actions that need a confirmed address (booking, messaging, publishing). */
    public static void requireVerifiedEmail(User user, String action) {
        if (!user.isEmailVerified()) {
            throw new ForbiddenException("Please confirm your email address before you " + action
                    + " - check your inbox, or resend the link from the banner at the top of the page");
        }
    }
}
