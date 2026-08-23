package com.accommodationfinder.controller;

import com.accommodationfinder.dto.PageResponse;
import com.accommodationfinder.dto.RoomDtos.*;
import com.accommodationfinder.model.RoomType;
import com.accommodationfinder.model.User;
import com.accommodationfinder.security.SecurityUtils;
import com.accommodationfinder.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@Tag(name = "Rooms", description = "Search, publish and manage accommodation listings")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    // ---------------------------------------------------------------- public

    @GetMapping
    @Operation(summary = "Search listings with optional filters, sorted and paginated")
    public PageResponse<RoomResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) Integer minSize,
            @RequestParam(required = false) Integer bedrooms,
            @RequestParam(required = false) Boolean availableOnly,
            @RequestParam(required = false) Boolean furnished,
            @RequestParam(required = false) Boolean billsIncluded,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso =
                    org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate availableFrom,
            @RequestParam(required = false) Long landlordId,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        RoomSearchCriteria criteria = new RoomSearchCriteria(
                query, city, minPrice, maxPrice, roomType, minSize, bedrooms,
                availableOnly, furnished, billsIncluded, availableFrom, landlordId);

        Page<RoomResponse> page = roomService.search(criteria, pageable);
        return PageResponse.of(page);
    }

    @GetMapping("/cities")
    @Operation(summary = "Distinct cities that currently have listings - powers the search autocomplete")
    public List<String> cities() {
        return roomService.listCities();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Full detail for one listing")
    public RoomResponse findById(@PathVariable Long id) {
        return roomService.findById(id);
    }

    // ---------------------------------------------------------------- landlord

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    @Operation(summary = "Listings owned by the signed-in landlord")
    public PageResponse<RoomResponse> myListings(
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        User me = SecurityUtils.requireCurrentUser();
        return PageResponse.of(roomService.findByLandlord(me.getId(), pageable));
    }

    @GetMapping("/mine/stats")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    @Operation(summary = "Counters for the landlord dashboard")
    public LandlordStats myStats() {
        return roomService.landlordStats(SecurityUtils.requireCurrentUser().getId());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    @Operation(summary = "Publish a new listing and broadcast it on /topic/rooms/new")
    public ResponseEntity<RoomResponse> create(@Valid @RequestBody RoomRequest request) {
        User me = SecurityUtils.requireCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.create(request, me));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update a listing you own")
    public RoomResponse update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return roomService.update(id, request, SecurityUtils.requireCurrentUser());
    }

    @PatchMapping("/{id}/availability")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Flip availability and push the change to every connected client in real time")
    public RoomResponse updateAvailability(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Boolean available = body.get("available");
        if (available == null) {
            throw new com.accommodationfinder.exception.BadRequestException(
                    "Body must contain an 'available' boolean");
        }
        return roomService.updateAvailability(id, available, SecurityUtils.requireCurrentUser());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a listing you own")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        roomService.delete(id, SecurityUtils.requireCurrentUser());
        return ResponseEntity.noContent().build();
    }
}
