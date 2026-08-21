package com.accommodationfinder.security;

import com.accommodationfinder.exception.ForbiddenException;
import com.accommodationfinder.model.Role;
import com.accommodationfinder.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Convenience access to the authenticated principal. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<User> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserDetails details) {
            return Optional.of(details.getUser());
        }
        return Optional.empty();
    }

    public static User requireCurrentUser() {
        return currentUser().orElseThrow(() -> new ForbiddenException("You must be signed in"));
    }

    public static boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    /** Owner or admin may proceed; anyone else gets a 403. */
    public static void requireOwnerOrAdmin(User actor, Long ownerId, String action) {
        if (actor == null || ownerId == null
                || (!ownerId.equals(actor.getId()) && !isAdmin(actor))) {
            throw new ForbiddenException("You are not allowed to " + action);
        }
    }
}
