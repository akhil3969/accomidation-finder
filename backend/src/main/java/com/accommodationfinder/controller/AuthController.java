package com.accommodationfinder.controller;

import com.accommodationfinder.dto.AuthDtos.AuthResponse;
import com.accommodationfinder.dto.AuthDtos.LoginRequest;
import com.accommodationfinder.dto.AuthDtos.RegisterRequest;
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

    @GetMapping("/me")
    @Operation(summary = "Return the signed-in user, or 401 if the token is missing or expired")
    public ResponseEntity<UserSummary> me() {
        return ResponseEntity.ok(UserSummary.from(SecurityUtils.requireCurrentUser()));
    }
}
