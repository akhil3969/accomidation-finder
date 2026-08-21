package com.accommodationfinder.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class ReviewDtos {

    private ReviewDtos() {
    }

    public record ReviewRequest(
            @NotNull(message = "Rating is required")
            @Min(value = 1, message = "Rating must be between 1 and 5")
            @Max(value = 5, message = "Rating must be between 1 and 5")
            Integer rating,

            @Size(max = 2000)
            String comment
    ) {
    }

    public record ReviewResponse(
            Long id,
            Long roomId,
            UserDtos.UserSummary author,
            Integer rating,
            String comment,
            LocalDateTime createdAt
    ) {
    }
}
