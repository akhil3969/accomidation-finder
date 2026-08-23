package com.accommodationfinder.dto;

import com.accommodationfinder.model.ReportReason;
import com.accommodationfinder.model.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** Payloads for the moderation queue, the analytics dashboard and data management. */
public final class AdminDtos {

    private AdminDtos() {
    }

    // ------------------------------------------------------------- moderation

    public record ReportRequest(
            Long roomId,
            Long reportedUserId,
            @NotNull(message = "Tell us what is wrong") ReportReason reason,
            @Size(max = 2000) String details
    ) {
    }

    public record ReportView(
            Long id,
            UserDtos.UserSummary reporter,
            Long roomId,
            String roomTitle,
            Integer roomRiskScore,
            UserDtos.UserSummary reportedUser,
            ReportReason reason,
            String reasonLabel,
            String details,
            ReportStatus status,
            String moderatorNote,
            String handledBy,
            LocalDateTime createdAt,
            LocalDateTime resolvedAt
    ) {
    }

    public record ResolveReportRequest(
            @NotNull ReportStatus status,
            @Size(max = 2000) String moderatorNote,
            /** Optionally hide the listing as part of resolving. */
            Boolean hideListing,
            Boolean banUser
    ) {
    }

    public record ModerationEntry(
            Long id,
            String moderator,
            String action,
            String targetType,
            Long targetId,
            String reason,
            LocalDateTime createdAt
    ) {
    }

    public record VerificationDecision(
            @NotNull Boolean approve,
            @Size(max = 500) String note
    ) {
    }

    // -------------------------------------------------------------- analytics

    /** One point on a time-series chart. */
    public record TimePoint(String date, long value) {
    }

    /** One slice or bar of a categorical chart. */
    public record CategoryCount(String label, long value) {
    }

    public record DashboardStats(
            long totalUsers,
            long tenants,
            long landlords,
            long newUsersThisWeek,
            long totalListings,
            long availableListings,
            long verifiedListings,
            long hiddenListings,
            long newListingsThisWeek,
            long totalBookings,
            long pendingBookings,
            long confirmedBookings,
            long bookingsThisWeek,
            long openReports,
            long pendingVerifications,
            long highRiskListings
    ) {
    }

    /** Everything the dashboard charts need, in one request. */
    public record AnalyticsBundle(
            DashboardStats stats,
            List<TimePoint> signupsPerDay,
            List<TimePoint> listingsPerDay,
            List<TimePoint> bookingsPerDay,
            List<CategoryCount> listingsByCity,
            List<CategoryCount> bookingsByStatus,
            List<CategoryCount> usersByNationality,
            List<CategoryCount> listingsByType,
            int days
    ) {
    }

    // ------------------------------------------------------- data management

    public record AdminUserRow(
            Long id,
            String name,
            String email,
            String role,
            String nationality,
            boolean enabled,
            String verificationStatus,
            long listingCount,
            long bookingCount,
            LocalDateTime createdAt
    ) {
    }

    public record AdminRoomRow(
            Long id,
            String title,
            String city,
            java.math.BigDecimal price,
            String landlordName,
            Long landlordId,
            boolean available,
            boolean verified,
            boolean hidden,
            Integer riskScore,
            long reportCount,
            LocalDateTime createdAt
    ) {
    }

    public record AidParameterRow(
            Long id,
            String key,
            java.math.BigDecimal value,
            String label,
            String source,
            String effectiveFrom,
            String lastVerified
    ) {
    }

    public record ChecklistItemRow(
            Long id,
            String key,
            String title,
            String explanation,
            String ifYouCannot,
            String officialLink,
            String category,
            Integer sortOrder,
            boolean nonEuOnly,
            boolean studentsOnly,
            boolean essential,
            boolean active
    ) {
    }
}
