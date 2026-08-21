package com.accommodationfinder.dto;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Profile, checklist and neighbourhood payloads for the arrival experience. */
public final class NewcomerDtos {

    private NewcomerDtos() {
    }

    /** Answers from the short wizard someone fills in when they first arrive. */
    public record ArrivalProfileRequest(
            @Size(max = 80) String nationality,
            Boolean euNational,
            LocalDate dateOfBirth,
            LocalDate arrivalDate,
            Boolean student,
            Boolean scholarshipHolder,
            @Size(max = 150) String university,
            BigDecimal monthlyIncome,
            Boolean hasFrenchGuarantor
    ) {
    }

    public record ArrivalProfile(
            String nationality,
            Boolean euNational,
            LocalDate dateOfBirth,
            LocalDate arrivalDate,
            Integer age,
            boolean student,
            boolean scholarshipHolder,
            String university,
            BigDecimal universityLatitude,
            BigDecimal universityLongitude,
            BigDecimal monthlyIncome,
            boolean hasFrenchGuarantor,
            boolean profileCompleted
    ) {
    }

    /** One row of the dossier checklist, already filtered to this person's situation. */
    public record ChecklistEntry(
            Long itemId,
            String key,
            String title,
            String explanation,
            String ifYouCannot,
            String officialLink,
            String category,
            boolean essential,
            boolean done,
            String note
    ) {
    }

    public record ChecklistView(
            int totalItems,
            int completedItems,
            int essentialRemaining,
            List<ChecklistEntry> entries,
            String encouragement
    ) {
    }

    /** What is actually around a listing - the things you cannot tell from an address. */
    public record NeighbourhoodInfo(
            Long roomId,
            String city,
            List<NearbyPlace> transport,
            List<NearbyPlace> essentials,
            Integer walkMinutesToTransport,
            CommuteEstimate commute,
            List<String> highlights,
            boolean dataAvailable,
            String dataNote
    ) {
    }

    public record NearbyPlace(
            String name,
            String type,
            String label,
            Integer metresAway,
            Integer walkMinutes,
            Double latitude,
            Double longitude
    ) {
    }

    /** Straight-line distance to where they study, plus a realistic travel estimate. */
    public record CommuteEstimate(
            String destinationName,
            Double kilometres,
            Integer estimatedMinutes,
            String method,
            String note
    ) {
    }

    /** Why a listing looks risky, in words a newcomer can act on. */
    public record SafetyReport(
            Long roomId,
            int riskScore,
            String riskLevel,
            List<String> warnings,
            List<String> reassurances,
            boolean landlordVerified,
            List<String> rulesToRemember
    ) {
    }

    public record GuideSummary(
            Long id,
            String slug,
            String title,
            String summary,
            String category,
            String icon,
            String officialLink
    ) {
    }

    public record GuideDetail(
            Long id,
            String slug,
            String title,
            String summary,
            String body,
            String category,
            String icon,
            String officialLink,
            String lastVerified
    ) {
    }
}
