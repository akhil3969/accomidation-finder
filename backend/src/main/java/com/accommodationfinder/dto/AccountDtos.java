package com.accommodationfinder.dto;

import com.accommodationfinder.dto.BookingDtos.BookingResponse;
import com.accommodationfinder.dto.MessageDtos.MessageResponse;
import com.accommodationfinder.dto.NotificationDtos.NotificationResponse;
import com.accommodationfinder.dto.ReviewDtos.ReviewResponse;
import com.accommodationfinder.dto.RoomDtos.RoomResponse;
import com.accommodationfinder.model.Role;
import com.accommodationfinder.model.VerificationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** The GDPR art. 15/20 export: everything held about one person, in one JSON document. */
public final class AccountDtos {

    private AccountDtos() {
    }

    public record PersonalDataExport(
            LocalDateTime exportedAt,
            String notice,
            Profile profile,
            List<BookingResponse> bookingsAsTenant,
            List<RoomResponse> listingsAsLandlord,
            List<MessageResponse> messages,
            List<ReviewResponse> reviewsWritten,
            List<Long> favouriteRoomIds,
            List<ChecklistEntry> documentChecklist,
            List<ReportFiled> reportsFiled,
            List<NotificationResponse> notifications
    ) {
    }

    /** Every column of the user row except the password hash, which is not "about" the person. */
    public record Profile(
            Long id,
            String name,
            String email,
            String phone,
            Role role,
            String avatarUrl,
            String bio,
            LocalDateTime createdAt,
            boolean emailVerified,
            boolean emailNotifications,
            LocalDateTime termsAcceptedAt,
            String termsVersion,
            String nationality,
            Boolean euNational,
            LocalDate dateOfBirth,
            LocalDate arrivalDate,
            boolean student,
            boolean scholarshipHolder,
            String university,
            BigDecimal monthlyIncome,
            boolean hasFrenchGuarantor,
            VerificationStatus landlordVerificationStatus
    ) {
    }

    public record ChecklistEntry(String item, boolean done, String note, LocalDateTime updatedAt) {
    }

    public record ReportFiled(Long id, Long roomId, Long reportedUserId, String reason, String details,
                              String status, LocalDateTime createdAt) {
    }
}
