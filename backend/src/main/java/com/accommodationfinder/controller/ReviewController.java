package com.accommodationfinder.controller;

import com.accommodationfinder.dto.ReviewDtos.ReviewRequest;
import com.accommodationfinder.dto.ReviewDtos.ReviewResponse;
import com.accommodationfinder.security.SecurityUtils;
import com.accommodationfinder.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Reviews", description = "Ratings left by tenants after a stay")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/room/{roomId}")
    @Operation(summary = "All reviews for a listing, newest first")
    public List<ReviewResponse> forRoom(@PathVariable Long roomId) {
        return reviewService.forRoom(roomId);
    }

    @PostMapping("/room/{roomId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Leave a review - requires a confirmed or completed stay")
    public ResponseEntity<ReviewResponse> create(@PathVariable Long roomId,
                                                 @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.create(roomId, request, SecurityUtils.requireCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete your own review")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reviewService.delete(id, SecurityUtils.requireCurrentUser());
        return ResponseEntity.noContent().build();
    }
}
