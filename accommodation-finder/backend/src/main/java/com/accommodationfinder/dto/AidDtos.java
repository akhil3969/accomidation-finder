package com.accommodationfinder.dto;

import com.accommodationfinder.model.HousingZone;

import java.math.BigDecimal;
import java.util.List;

/** Everything the newcomer side of the app returns about money and eligibility. */
public final class AidDtos {

    private AidDtos() {
    }

    /**
     * One line of the "how we got this number" breakdown.
     *
     * <p>The whole point of showing these is that a student arriving from outside France has
     * no idea CAF exists, let alone how it decides. A number with no explanation is not much
     * more useful than no number.
     */
    public record CalculationStep(
            String label,
            String value,
            String explanation
    ) {
    }

    /**
     * What housing benefit is likely to be worth on a given listing.
     *
     * <p>Deliberately a range rather than a single figure: the exact amount depends on
     * details CAF asks for and we do not (savings, exact household composition, the precise
     * date you move in). A range you can trust beats a precise number you cannot.
     */
    public record AplEstimate(
            boolean likelyEligible,
            BigDecimal monthlyRent,
            BigDecimal monthlyCharges,
            HousingZone zone,
            String zoneLabel,
            BigDecimal estimateLow,
            BigDecimal estimateHigh,
            BigDecimal estimateMid,
            /** Rent minus the aid - the number that actually matters when comparing flats. */
            BigDecimal effectiveMonthlyCost,
            BigDecimal rentConsidered,
            boolean rentAboveCeiling,
            List<CalculationStep> breakdown,
            List<String> notes,
            String officialSimulatorUrl,
            String parametersVerifiedOn
    ) {
    }

    /** Whether the free state guarantor will cover this listing for this person. */
    public record VisaleCheck(
            boolean eligible,
            List<String> passed,
            List<String> blockers,
            BigDecimal rentCeilingApplied,
            BigDecimal rentIncludingCharges,
            String whatItIs,
            String whyItMatters,
            List<String> howToApply,
            String officialUrl
    ) {
    }

    /** The combined answer shown on a listing: aid, guarantor, and the real monthly cost. */
    public record AffordabilitySummary(
            Long roomId,
            BigDecimal advertisedRent,
            BigDecimal charges,
            BigDecimal totalBeforeAid,
            BigDecimal estimatedAid,
            BigDecimal estimatedRealCost,
            boolean visaleEligible,
            BigDecimal upfrontCost,
            List<CalculationStep> upfrontBreakdown,
            String summarySentence
    ) {
    }

    /** Request body when someone wants an estimate without being signed in. */
    public record AidQuery(
            BigDecimal rent,
            BigDecimal charges,
            String postcode,
            Boolean student,
            Boolean scholarship,
            Boolean sharedFlat,
            BigDecimal monthlyIncome,
            Integer age
    ) {
    }
}
