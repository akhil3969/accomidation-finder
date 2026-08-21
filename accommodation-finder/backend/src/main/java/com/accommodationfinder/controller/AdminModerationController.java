package com.accommodationfinder.controller;

import com.accommodationfinder.dto.AdminDtos.*;
import com.accommodationfinder.dto.PageResponse;
import com.accommodationfinder.dto.UserDtos.UserSummary;
import com.accommodationfinder.model.ReportStatus;
import com.accommodationfinder.repository.RoomRepository;
import com.accommodationfinder.security.SecurityUtils;
import com.accommodationfinder.service.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** The moderation queue: reports, verifications, and the record of what was done. */
@RestController
@RequestMapping("/api/admin/moderation")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin moderation", description = "Reports, verification and the audit trail")
public class AdminModerationController {

    private final ModerationService moderationService;
    private final RoomRepository roomRepository;

    public AdminModerationController(ModerationService moderationService, RoomRepository roomRepository) {
        this.moderationService = moderationService;
        this.roomRepository = roomRepository;
    }

    @GetMapping("/reports")
    @Operation(summary = "Reported listings and users, newest first")
    public PageResponse<ReportView> reports(
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(moderationService.queue(status, pageable));
    }

    @PatchMapping("/reports/{id}")
    @Operation(summary = "Resolve a report, optionally hiding the listing or banning the user")
    public ReportView resolve(@PathVariable Long id,
                              @Valid @RequestBody ResolveReportRequest decision) {
        return moderationService.resolve(id, decision, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/verifications")
    @Operation(summary = "Landlords waiting for an identity check")
    public List<UserSummary> verifications() {
        return moderationService.awaitingVerification();
    }

    @PatchMapping("/verifications/{userId}")
    @Operation(summary = "Approve or reject a landlord's verification")
    public UserSummary decide(@PathVariable Long userId,
                              @Valid @RequestBody VerificationDecision decision) {
        return moderationService.decideVerification(userId, decision, SecurityUtils.requireCurrentUser());
    }

    @PatchMapping("/rooms/{id}/hidden")
    @Operation(summary = "Hide a listing from search, or put it back")
    public ResponseEntity<Void> setHidden(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean hidden = Boolean.TRUE.equals(body.get("hidden"));
        String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));
        moderationService.setListingHidden(id, hidden, reason, SecurityUtils.requireCurrentUser());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/rooms/{id}/verified")
    @Operation(summary = "Mark a listing as checked by our team")
    public ResponseEntity<Void> setVerified(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        moderationService.setListingVerified(id, Boolean.TRUE.equals(body.get("verified")),
                SecurityUtils.requireCurrentUser());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/high-risk")
    @Operation(summary = "Listings the scam scorer is most worried about")
    public List<AdminRoomRow> highRisk() {
        return roomRepository.findTop20ByRiskScoreGreaterThanOrderByRiskScoreDesc(25).stream()
                .map(room -> new AdminRoomRow(
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
                        0L,
                        room.getCreatedAt()))
                .toList();
    }

    @GetMapping("/audit")
    @Operation(summary = "Every moderation decision, with who made it")
    public PageResponse<ModerationEntry> audit(@PageableDefault(size = 30) Pageable pageable) {
        return PageResponse.of(moderationService.auditLog(pageable));
    }
}
