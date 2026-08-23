package com.accommodationfinder.controller;

import com.accommodationfinder.dto.AidDtos.*;
import com.accommodationfinder.model.HousingZone;
import com.accommodationfinder.model.Room;
import com.accommodationfinder.model.User;
import com.accommodationfinder.model.RoomType;
import com.accommodationfinder.security.SecurityUtils;
import com.accommodationfinder.service.AidParameters;
import com.accommodationfinder.service.HousingAidService;
import com.accommodationfinder.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Money questions: what will this really cost me, and can I get a guarantor?
 *
 * <p>Deliberately open to anonymous callers. Somebody deciding whether to move to France at
 * all should be able to get an answer before creating an account.
 */
@RestController
@RequestMapping("/api/aid")
@Tag(name = "Housing aid", description = "Housing benefit estimates and guarantor eligibility")
public class AidController {

    private final HousingAidService housingAidService;
    private final RoomService roomService;

    public AidController(HousingAidService housingAidService, RoomService roomService) {
        this.housingAidService = housingAidService;
        this.roomService = roomService;
    }

    @GetMapping("/room/{roomId}")
    @Operation(summary = "Housing benefit estimate for one listing, personalised if signed in")
    public AplEstimate forRoom(@PathVariable Long roomId) {
        Room room = roomService.getEntity(roomId);
        User me = SecurityUtils.currentUser().orElse(null);
        return housingAidService.estimateForRoom(room, me);
    }

    @GetMapping("/room/{roomId}/summary")
    @Operation(summary = "The one-glance answer: real monthly cost, guarantor, money needed upfront")
    public AffordabilitySummary summary(@PathVariable Long roomId) {
        Room room = roomService.getEntity(roomId);
        return housingAidService.summarise(room, SecurityUtils.currentUser().orElse(null));
    }

    @PostMapping("/estimate")
    @Operation(summary = "Estimate for any rent and postcode, without needing an account")
    public AplEstimate estimate(@RequestBody AidQuery query) {
        HousingZone zone = housingAidService.zoneForPostcode(query.postcode());
        return housingAidService.estimate(
                query.rent(),
                query.charges(),
                zone,
                Boolean.TRUE.equals(query.sharedFlat()),
                query.student() == null || query.student(),
                Boolean.TRUE.equals(query.scholarship()),
                query.monthlyIncome());
    }

    @PostMapping("/visale")
    @Operation(summary = "Check the free state guarantor against a rent and a postcode")
    public VisaleCheck visale(@RequestBody AidQuery query) {
        Integer age = query.age();
        if (age == null) {
            age = SecurityUtils.currentUser()
                    .map(housingAidService::ageOf)
                    .orElse(null);
        }
        return housingAidService.checkVisale(
                query.rent(),
                query.charges(),
                query.postcode(),
                query.student() == null || query.student(),
                age);
    }

    @GetMapping("/visale/room/{roomId}")
    @Operation(summary = "Guarantor check for a specific listing")
    public VisaleCheck visaleForRoom(@PathVariable Long roomId) {
        Room room = roomService.getEntity(roomId);
        User me = SecurityUtils.currentUser().orElse(null);

        BigDecimal charges = Boolean.TRUE.equals(room.getBillsIncluded())
                ? BigDecimal.ZERO
                : (room.getChargesAmount() == null ? BigDecimal.ZERO : room.getChargesAmount());

        return housingAidService.checkVisale(
                room.getPrice(),
                charges,
                room.getPostalCode(),
                me == null || me.isStudent(),
                me == null ? null : housingAidService.ageOf(me));
    }

    @GetMapping("/zone")
    @Operation(summary = "Which CAF rent-ceiling zone a postcode falls in")
    public java.util.Map<String, String> zone(@RequestParam String postcode) {
        HousingZone zone = housingAidService.zoneForPostcode(postcode);
        return java.util.Map.of(
                "zone", zone.name(),
                "label", housingAidService.zoneLabel(zone));
    }
}
