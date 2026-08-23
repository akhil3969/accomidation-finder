package com.accommodationfinder.controller;

import com.accommodationfinder.dto.NewcomerDtos.*;
import com.accommodationfinder.model.Room;
import com.accommodationfinder.security.SecurityUtils;
import com.accommodationfinder.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** The arrival experience: your profile, your paperwork, and what is around a flat. */
@RestController
@RequestMapping("/api/newcomer")
@Tag(name = "Newcomer", description = "Arrival profile, dossier checklist, neighbourhood and safety")
public class NewcomerController {

    private final NewcomerProfileService profileService;
    private final ChecklistService checklistService;
    private final NeighbourhoodService neighbourhoodService;
    private final ScamDetectionService scamDetectionService;
    private final GuideService guideService;
    private final RoomService roomService;

    public NewcomerController(NewcomerProfileService profileService,
                              ChecklistService checklistService,
                              NeighbourhoodService neighbourhoodService,
                              ScamDetectionService scamDetectionService,
                              GuideService guideService,
                              RoomService roomService) {
        this.profileService = profileService;
        this.checklistService = checklistService;
        this.neighbourhoodService = neighbourhoodService;
        this.scamDetectionService = scamDetectionService;
        this.guideService = guideService;
        this.roomService = roomService;
    }

    // ------------------------------------------------------------------ profile

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "The few facts we use to personalise everything else")
    public ArrivalProfile profile() {
        return profileService.get(SecurityUtils.requireCurrentUser());
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Save the arrival wizard answers")
    public ArrivalProfile saveProfile(@Valid @RequestBody ArrivalProfileRequest request) {
        return profileService.save(SecurityUtils.requireCurrentUser(), request);
    }

    // ---------------------------------------------------------------- checklist

    @GetMapping("/checklist")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Your dossier checklist, filtered to your situation")
    public ChecklistView checklist() {
        return checklistService.forUser(SecurityUtils.requireCurrentUser());
    }

    @PatchMapping("/checklist/{itemId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tick a document off, or add a note to it")
    public ChecklistView toggle(@PathVariable Long itemId, @RequestBody Map<String, Object> body) {
        boolean done = Boolean.TRUE.equals(body.get("done"));
        String note = body.get("note") == null ? null : String.valueOf(body.get("note"));
        return checklistService.toggle(SecurityUtils.requireCurrentUser(), itemId, done, note);
    }

    // ------------------------------------------------------------ neighbourhood

    @GetMapping("/neighbourhood/{roomId}")
    @Operation(summary = "Transport, shops and travel time to your campus, from OpenStreetMap")
    public NeighbourhoodInfo neighbourhood(@PathVariable Long roomId) {
        Room room = roomService.getEntity(roomId);
        return neighbourhoodService.forRoom(room, SecurityUtils.currentUser().orElse(null));
    }

    // ------------------------------------------------------------------ safety

    @GetMapping("/safety/{roomId}")
    @Operation(summary = "Why this listing does or does not look trustworthy")
    public SafetyReport safety(@PathVariable Long roomId) {
        return scamDetectionService.reportFor(roomService.getEntity(roomId));
    }

    // ------------------------------------------------------------------ guides

    @GetMapping("/guides")
    @Operation(summary = "Plain-language explainers for people new to the French system")
    public List<GuideSummary> guides() {
        return guideService.published();
    }

    @GetMapping("/guides/{slug}")
    @Operation(summary = "One guide in full")
    public GuideDetail guide(@PathVariable String slug) {
        return guideService.bySlug(slug);
    }
}
