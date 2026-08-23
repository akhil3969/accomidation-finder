package com.accommodationfinder.service;

import com.accommodationfinder.dto.UserDtos.ChangePasswordRequest;
import com.accommodationfinder.dto.UserDtos.UpdateProfileRequest;
import com.accommodationfinder.dto.UserDtos.UserSummary;
import com.accommodationfinder.exception.BadRequestException;
import com.accommodationfinder.exception.ResourceNotFoundException;
import com.accommodationfinder.model.Role;
import com.accommodationfinder.model.User;
import com.accommodationfinder.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserSummary profile(Long userId) {
        return UserSummary.from(userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId)));
    }

    @Transactional(readOnly = true)
    public UserSummary publicProfile(Long userId) {
        return UserSummary.publicProfile(userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId)));
    }

    @Transactional
    public UserSummary updateProfile(User current, UpdateProfileRequest request) {
        User user = userRepository.findById(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", current.getId()));

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone().trim());
        }
        if (request.bio() != null) {
            user.setBio(request.bio().trim());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl().trim());
        }
        return UserSummary.from(userRepository.save(user));
    }

    @Transactional
    public void changePassword(User current, ChangePasswordRequest request) {
        User user = userRepository.findById(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", current.getId()));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (request.newPassword() == null || request.newPassword().length() < 8) {
            throw new BadRequestException("New password must be at least 8 characters");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    /** Lets a tenant start publishing listings without creating a second account. */
    @Transactional
    public UserSummary becomeLandlord(User current) {
        User user = userRepository.findById(current.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", current.getId()));
        if (user.getRole() == Role.TENANT) {
            user.setRole(Role.LANDLORD);
            userRepository.save(user);
        }
        return UserSummary.from(user);
    }

    // ------------------------------------------------------------- admin only

    @Transactional(readOnly = true)
    public List<UserSummary> listAll() {
        return userRepository.findAll().stream().map(UserSummary::from).toList();
    }

    @Transactional
    public UserSummary setEnabled(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setEnabled(enabled);
        return UserSummary.from(userRepository.save(user));
    }
}
