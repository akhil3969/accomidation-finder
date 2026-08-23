package com.accommodationfinder.dto;

import com.accommodationfinder.model.Role;
import jakarta.validation.constraints.*;

/** Request and response payloads for registration and login. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Name is required")
            @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
            String name,

            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            @Size(max = 150)
            String email,

            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
            String password,

            @Size(max = 30)
            String phone,

            /** TENANT when omitted. ADMIN can never be self-assigned. */
            Role role
    ) {
    }

    public record LoginRequest(
            @NotBlank(message = "Email is required")
            @Email(message = "Email must be valid")
            String email,

            @NotBlank(message = "Password is required")
            String password
    ) {
    }

    public record AuthResponse(
            String token,
            String tokenType,
            long expiresInMs,
            UserDtos.UserSummary user
    ) {
        public static AuthResponse of(String token, long expiresInMs, UserDtos.UserSummary user) {
            return new AuthResponse(token, "Bearer", expiresInMs, user);
        }
    }
}
