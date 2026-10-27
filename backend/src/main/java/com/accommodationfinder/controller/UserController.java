package com.accommodationfinder.controller;

import com.accommodationfinder.dto.AccountDtos.PersonalDataExport;
import com.accommodationfinder.dto.UserDtos.ChangePasswordRequest;
import com.accommodationfinder.dto.UserDtos.DeleteAccountRequest;
import com.accommodationfinder.dto.UserDtos.UpdateProfileRequest;
import com.accommodationfinder.dto.UserDtos.UserSummary;
import com.accommodationfinder.security.SecurityUtils;
import com.accommodationfinder.service.AccountService;
import com.accommodationfinder.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Profile management")
public class UserController {

    private final UserService userService;
    private final AccountService accountService;

    public UserController(UserService userService, AccountService accountService) {
        this.userService = userService;
        this.accountService = accountService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Full profile of the signed-in user")
    public UserSummary me() {
        return userService.profile(SecurityUtils.requireCurrentUser().getId());
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update name, phone, bio or avatar")
    public UserSummary updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(SecurityUtils.requireCurrentUser(), request);
    }

    @PostMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change your password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(SecurityUtils.requireCurrentUser(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/become-landlord")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upgrade a tenant account so it can publish listings")
    public UserSummary becomeLandlord() {
        return userService.becomeLandlord(SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/me/export")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Download everything held about you as JSON (GDPR right of access and portability)")
    public ResponseEntity<PersonalDataExport> export() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"accomfinder-my-data.json\"")
                .body(accountService.export(SecurityUtils.requireCurrentUser().getId()));
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Erase your account and personal data (GDPR right to erasure). Needs your password")
    public ResponseEntity<Void> deleteMe(@Valid @RequestBody DeleteAccountRequest request) {
        accountService.deleteOwnAccount(SecurityUtils.requireCurrentUser(), request.password());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Public profile of any user")
    public UserSummary publicProfile(@PathVariable Long id) {
        return userService.publicProfile(id);
    }
}
