package com.accommodationfinder.service;

import com.accommodationfinder.dto.AuthDtos.AuthResponse;
import com.accommodationfinder.dto.AuthDtos.LoginRequest;
import com.accommodationfinder.dto.AuthDtos.RegisterRequest;
import com.accommodationfinder.dto.UserDtos.UserSummary;
import com.accommodationfinder.exception.ConflictException;
import com.accommodationfinder.model.Role;
import com.accommodationfinder.model.User;
import com.accommodationfinder.repository.UserRepository;
import com.accommodationfinder.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
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

        User saved = userRepository.save(user);
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
}
