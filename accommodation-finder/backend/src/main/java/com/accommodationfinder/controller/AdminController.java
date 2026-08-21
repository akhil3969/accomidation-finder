package com.accommodationfinder.controller;

import com.accommodationfinder.dto.AdminDtos.*;
import com.accommodationfinder.dto.PageResponse;
import com.accommodationfinder.dto.UserDtos.UserSummary;
import com.accommodationfinder.exception.BadRequestException;
import com.accommodationfinder.exception.ResourceNotFoundException;
import com.accommodationfinder.model.Role;
import com.accommodationfinder.model.User;
import com.accommodationfinder.repository.*;
import com.accommodationfinder.security.SecurityUtils;
import com.accommodationfinder.service.AnalyticsService;
import com.accommodationfinder.service.ScamDetectionService;
import com.accommodationfinder.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Dashboard numbers and day-to-day management of users and listings. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Analytics and data management")
public class AdminController {

    private final AnalyticsService analyticsService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final ReportRepository reportRepository;
    private final ScamDetectionService scamDetectionService;

    public AdminController(AnalyticsService analyticsService,
                           UserService userService,
                           UserRepository userRepository,
                           RoomRepository roomRepository,
                           BookingRepository bookingRepository,
                           ReportRepository reportRepository,
                           ScamDetectionService scamDetectionService) {
        this.analyticsService = analyticsService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.reportRepository = reportRepository;
        this.scamDetectionService = scamDetectionService;
    }

    // ---------------------------------------------------------------- analytics

    @GetMapping("/stats")
    @Operation(summary = "Headline counters for the dashboard tiles")
    public DashboardStats stats() {
        return analyticsService.stats();
    }

    @GetMapping("/analytics")
    @Operation(summary = "Everything the dashboard charts need, in one request")
    public AnalyticsBundle analytics(@RequestParam(defaultValue = "30") int days) {
        return analyticsService.bundle(days);
    }

    // -------------------------------------------------------------------- users

    @GetMapping("/users")
    @Operation(summary = "Every account, with the counts that matter for moderation")
    public List<AdminUserRow> users() {
        return userRepository.findAll().stream()
                .map(user -> new AdminUserRow(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole().name(),
                        user.getNationality(),
                        user.isEnabled(),
                        user.getVerificationStatus().name(),
                        roomRepository.countByLandlordId(user.getId()),
                        bookingRepository.countByTenantIdAndStatus(
                                user.getId(), com.accommodationfinder.model.BookingStatus.PENDING),
                        user.getCreatedAt()))
                .toList();
    }

    @PatchMapping("/users/{id}/enabled")
    @Operation(summary = "Enable or disable an account")
    public UserSummary setEnabled(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return userService.setEnabled(id, Boolean.TRUE.equals(body.get("enabled")));
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Change someone's role")
    public UserSummary setRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        User me = SecurityUtils.requireCurrentUser();
        if (target.getId().equals(me.getId())) {
            // Removing your own admin rights locks you out of the panel you are standing in.
            throw new BadRequestException("You cannot change your own role");
        }

        try {
            target.setRole(Role.valueOf(body.get("role")));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BadRequestException("Role must be TENANT, LANDLORD or ADMIN");
        }
        return UserSummary.from(userRepository.save(target));
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Delete an account and everything attached to it")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        User me = SecurityUtils.requireCurrentUser();
        if (me.getId().equals(id)) {
            throw new BadRequestException("You cannot delete your own account from here");
        }
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------- rooms

    @GetMapping("/rooms")
    @Operation(summary = "Every listing, including hidden ones, with risk and report counts")
    public PageResponse<AdminRoomRow> rooms(
            @PageableDefault(size = 25, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return PageResponse.of(roomRepository.findAll(pageable).map(room -> new AdminRoomRow(
                room.getId(),
                room.getTitle(),
                room.getCity(),
                room.getPrice(),
                room.getLandlord() == null ? null : room.getLandlord().getName(),
                room.getLandlord() == null ? null : room.getLandlord().getId(),
                Boolean.TRUE.equals(room.getAvailable()),
                room.isVerified(),
                room.isHidden(),
                room.getRiskScore(),
                reportRepository.countByRoomId(room.getId()),
                room.getCreatedAt())));
    }

    @PostMapping("/rooms/rescore")
    @Operation(summary = "Re-run the scam scorer over every listing")
    public Map<String, Object> rescoreAll() {
        List<com.accommodationfinder.model.Room> rooms = roomRepository.findAll();
        rooms.forEach(room -> roomRepository.save(scamDetectionService.rescore(room)));
        long flagged = rooms.stream()
                .filter(room -> room.getRiskScore() != null && room.getRiskScore() >= 25)
                .count();
        return Map.of("scored", rooms.size(), "flagged", flagged);
    }

    @DeleteMapping("/rooms/{id}")
    @Operation(summary = "Delete a listing outright")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        if (!roomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Room", id);
        }
        roomRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
