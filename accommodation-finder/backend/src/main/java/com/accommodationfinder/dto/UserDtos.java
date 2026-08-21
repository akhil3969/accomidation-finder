package com.accommodationfinder.dto;

import com.accommodationfinder.model.Role;
import com.accommodationfinder.model.User;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class UserDtos {

    private UserDtos() {
    }

    /** Safe public projection of a user - never contains the password hash. */
    public record UserSummary(
            Long id,
            String name,
            String email,
            String phone,
            Role role,
            String avatarUrl,
            String bio,
            LocalDateTime createdAt
    ) {
        public static UserSummary from(User user) {
            if (user == null) {
                return null;
            }
            return new UserSummary(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getRole(),
                    user.getAvatarUrl(),
                    user.getBio(),
                    user.getCreatedAt());
        }

        /** Minimal projection used inside room cards and chat threads. */
        public static UserSummary publicProfile(User user) {
            if (user == null) {
                return null;
            }
            return new UserSummary(
                    user.getId(),
                    user.getName(),
                    null,
                    null,
                    user.getRole(),
                    user.getAvatarUrl(),
                    user.getBio(),
                    null);
        }
    }

    public record UpdateProfileRequest(
            @Size(min = 2, max = 100) String name,
            @Size(max = 30) String phone,
            @Size(max = 500) String bio,
            @Size(max = 500) String avatarUrl
    ) {
    }

    public record ChangePasswordRequest(
            String currentPassword,
            @Size(min = 8, max = 72) String newPassword
    ) {
    }
}
