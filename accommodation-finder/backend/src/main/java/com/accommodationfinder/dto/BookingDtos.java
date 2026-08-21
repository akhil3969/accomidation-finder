package com.accommodationfinder.dto;

import com.accommodationfinder.model.BookingStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class BookingDtos {

    private BookingDtos() {
    }

    public record BookingRequest(
            @NotNull(message = "Room id is required")
            Long roomId,

            @NotNull(message = "Check-in date is required")
            @FutureOrPresent(message = "Check-in cannot be in the past")
            LocalDate checkIn,

            @NotNull(message = "Check-out date is required")
            @Future(message = "Check-out must be in the future")
            LocalDate checkOut,

            @Min(1) @Max(20)
            Integer guests,

            @Size(max = 2000)
            String message
    ) {
    }

    /** Landlord decision on a pending request. */
    public record BookingDecisionRequest(
            @NotNull(message = "Status is required")
            BookingStatus status,

            @Size(max = 2000)
            String response
    ) {
    }

    public record BookingResponse(
            Long id,
            Long roomId,
            String roomTitle,
            String roomCity,
            String roomImage,
            BigDecimal roomPrice,
            UserDtos.UserSummary tenant,
            UserDtos.UserSummary landlord,
            LocalDate checkIn,
            LocalDate checkOut,
            Integer guests,
            String message,
            String landlordResponse,
            BookingStatus status,
            BigDecimal totalPrice,
            LocalDateTime requestedAt,
            LocalDateTime updatedAt
    ) {
    }

    /** Blocked date range shown on the room calendar. */
    public record BookedRange(LocalDate checkIn, LocalDate checkOut, BookingStatus status) {
    }
}
