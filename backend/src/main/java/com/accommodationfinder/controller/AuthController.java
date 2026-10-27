package com.accommodationfinder.controller;

import com.accommodationfinder.dto.AuthDtos.AuthResponse;
import com.accommodationfinder.dto.AuthDtos.ForgotPasswordRequest;
import com.accommodationfinder.dto.AuthDtos.LoginRequest;
import com.accommodationfinder.dto.AuthDtos.RegisterRequest;
import com.accommodationfinder.dto.AuthDtos.ResetPasswordRequest;
import com.accommodationfinder.dto.AuthDtos.TokenRequest;
import com.accommodationfinder.dto.UserDtos.UserSummary;
import com.accommodationfinder.security.SecurityUtils;
import com.accommodationfinder.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Register, sign in and inspect the current session")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Create an account and return a JWT")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange email and password for a JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Confirm an email address with the token from the verification email")
    public UserSummary verifyEmail(@Valid @RequestBody TokenRequest request) {
        return authService.verifyEmail(request.token());
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Send a fresh verification email to the signed-in user")
    public ResponseEntity<Void> resendVerification() {
        authService.resendVerification(SecurityUtils.requireCurrentUser());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Email a password-reset link. Always 204, whether or not the account exists")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Set a new password with the token from the reset email; signs out every device")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Return the signed-in user, or 401 if the token is missing or expired")
    public ResponseEntity<UserSummary> me() {
        return ResponseEntity.ok(UserSummary.from(SecurityUtils.requireCurrentUser()));
    }
}
