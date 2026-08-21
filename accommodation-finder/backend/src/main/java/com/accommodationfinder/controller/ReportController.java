package com.accommodationfinder.controller;

import com.accommodationfinder.dto.AdminDtos.ReportRequest;
import com.accommodationfinder.dto.AdminDtos.ReportView;
import com.accommodationfinder.model.ReportReason;
import com.accommodationfinder.security.SecurityUtils;
import com.accommodationfinder.service.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Letting users flag what looks wrong. The other half of scam prevention. */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Flagging suspicious listings and behaviour")
public class ReportController {

    private final ModerationService moderationService;

    public ReportController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Report a listing or a user to the moderation team")
    public ResponseEntity<ReportView> submit(@Valid @RequestBody ReportRequest request) {
        ReportView view = moderationService.submit(request, SecurityUtils.requireCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @GetMapping("/reasons")
    @Operation(summary = "The reasons a user can pick, with readable labels")
    public List<Map<String, String>> reasons() {
        return Arrays.stream(ReportReason.values())
                .map(reason -> Map.of("value", reason.name(), "label", label(reason)))
                .toList();
    }

    private String label(ReportReason reason) {
        return switch (reason) {
            case SUSPECTED_SCAM -> "I think this is a scam";
            case ASKED_FOR_MONEY_UPFRONT -> "They asked for money before a viewing";
            case LISTING_DOES_NOT_EXIST -> "This flat does not exist";
            case MISLEADING_PHOTOS -> "The photos are not of this flat";
            case DISCRIMINATION -> "I was discriminated against";
            case ABUSIVE_BEHAVIOUR -> "Abusive or threatening behaviour";
            case ALREADY_RENTED -> "It is already rented out";
            case OTHER -> "Something else";
        };
    }
}
