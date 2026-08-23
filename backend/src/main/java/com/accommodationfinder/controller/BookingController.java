package com.accommodationfinder.controller;

import com.accommodationfinder.dto.BookingDtos.*;
import com.accommodationfinder.dto.PageResponse;
import com.accommodationfinder.model.User;
import com.accommodationfinder.security.SecurityUtils;
import com.accommodationfinder.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Bookings", description = "Request, accept, reject and cancel stays")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @Operation(summary = "Request a stay - notifies the landlord on /user/queue/bookings")
    public ResponseEntity<BookingResponse> request(@Valid @RequestBody BookingRequest request) {
        User me = SecurityUtils.requireCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.request(request, me));
    }

    @GetMapping("/mine")
    @Operation(summary = "Bookings the signed-in user has requested")
    public PageResponse<BookingResponse> mine(@PageableDefault(size = 10) Pageable pageable) {
        User me = SecurityUtils.requireCurrentUser();
        return PageResponse.of(bookingService.myBookings(me.getId(), pageable));
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    @Operation(summary = "Requests received on the signed-in landlord's listings")
    public PageResponse<BookingResponse> requests(@PageableDefault(size = 10) Pageable pageable) {
        User me = SecurityUtils.requireCurrentUser();
        return PageResponse.of(bookingService.requestsForLandlord(me.getId(), pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "One booking - visible to its tenant, the landlord and admins")
    public BookingResponse findById(@PathVariable Long id) {
        return bookingService.findById(id, SecurityUtils.requireCurrentUser());
    }

    @PatchMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    @Operation(summary = "Confirm, reject or complete a booking request")
    public BookingResponse decide(@PathVariable Long id,
                                  @Valid @RequestBody BookingDecisionRequest decision) {
        return bookingService.decide(id, decision, SecurityUtils.requireCurrentUser());
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel your own booking request")
    public BookingResponse cancel(@PathVariable Long id) {
        return bookingService.cancel(id, SecurityUtils.requireCurrentUser());
    }

    @GetMapping("/room/{roomId}/ranges")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Date ranges already held on a listing, for the availability calendar")
    public List<BookedRange> bookedRanges(@PathVariable Long roomId) {
        return bookingService.bookedRanges(roomId);
    }
}
