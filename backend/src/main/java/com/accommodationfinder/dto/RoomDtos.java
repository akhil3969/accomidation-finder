package com.accommodationfinder.dto;

import com.accommodationfinder.model.RoomType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public final class RoomDtos {

    private RoomDtos() {
    }

    /** Payload for creating or fully updating a listing. */
    public record RoomRequest(
            @NotBlank(message = "Title is required")
            @Size(max = 200)
            String title,

            @Size(max = 5000)
            String description,

            @NotBlank(message = "City is required")
            @Size(max = 100)
            String city,

            @Size(max = 255) String address,
            @Size(max = 20) String postalCode,
            @Size(max = 100) String country,

            @NotNull(message = "Price is required")
            @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
            @Digits(integer = 8, fraction = 2)
            BigDecimal price,

            @DecimalMin(value = "0.0") @Digits(integer = 8, fraction = 2)
            BigDecimal deposit,

            /** Monthly bills on top of the rent, when they are not included. */
            @DecimalMin(value = "0.0") @Digits(integer = 8, fraction = 2)
            BigDecimal chargesAmount,

            @Min(1) @Max(1000) Integer sizeSqm,
            @Min(0) @Max(20) Integer bedrooms,
            @Min(0) @Max(20) Integer bathrooms,
            @Min(1) @Max(20) Integer maxGuests,

            Boolean available,
            Boolean furnished,
            Boolean billsIncluded,
            LocalDate availableFrom,
            @Min(0) @Max(60) Integer minStayMonths,

            @NotNull(message = "Room type is required")
            RoomType roomType,

            @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,

            List<@Size(max = 500) String> images,
            Set<@Size(max = 60) String> amenities
    ) {
    }

    /** What the API returns for a listing. */
    public record RoomResponse(
            Long id,
            String title,
            String description,
            String city,
            String address,
            String postalCode,
            String country,
            BigDecimal price,
            BigDecimal deposit,
            Integer sizeSqm,
            Integer bedrooms,
            Integer bathrooms,
            Integer maxGuests,
            Boolean available,
            Boolean furnished,
            Boolean billsIncluded,
            LocalDate availableFrom,
            Integer minStayMonths,
            RoomType roomType,
            BigDecimal latitude,
            BigDecimal longitude,
            List<String> images,
            Set<String> amenities,
            UserDtos.UserSummary landlord,
            Double averageRating,
            Long reviewCount,
            Boolean favorite,
            // --- what a newcomer needs to see on the card itself ---
            BigDecimal chargesAmount,
            BigDecimal totalMonthlyCost,
            com.accommodationfinder.model.HousingZone housingZone,
            boolean verified,
            boolean landlordVerified,
            Integer riskScore,
            String riskLevel,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    /** Search filters accepted by GET /api/rooms. */
    public record RoomSearchCriteria(
            String query,
            String city,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            RoomType roomType,
            Integer minSize,
            Integer bedrooms,
            Boolean availableOnly,
            Boolean furnished,
            Boolean billsIncluded,
            LocalDate availableFrom,
            Long landlordId
    ) {
    }

    /** Real-time payload pushed on /topic/rooms/availability. */
    public record AvailabilityEvent(Long roomId, Boolean available, String title, String city) {
    }

    /** Landlord dashboard counters. */
    public record LandlordStats(
            long totalListings,
            long availableListings,
            long pendingBookings,
            long confirmedBookings,
            long unreadMessages
    ) {
    }
}
