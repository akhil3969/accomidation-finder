package com.accommodationfinder.service;

import com.accommodationfinder.dto.AidDtos.*;
import com.accommodationfinder.model.HousingZone;
import com.accommodationfinder.model.Room;
import com.accommodationfinder.model.RoomType;
import com.accommodationfinder.model.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Works out what a listing really costs somebody once French housing aid is taken into
 * account, and whether the free state guarantor will cover them.
 *
 * <p>Why this exists: a student arriving from outside the EU sees a EUR 750 studio and
 * compares it against their budget. What they do not know is that CAF will very likely pay
 * a chunk of it, that the state offers a free guarantor when no French relative can sign
 * for them, and that both have to be applied for. People routinely rule out flats they
 * could afford, or pay an agency for a guarantee they could have had for nothing.
 *
 * <p>What this is not: a decision. CAF decides. Everything here is labelled as an estimate
 * and links to the official simulator, and the breakdown shows every figure used so the
 * number can be checked rather than trusted blindly.
 */
@Service
public class HousingAidService {

    private static final String OFFICIAL_SIMULATOR =
            "https://www.mesdroitssociaux.gouv.fr/accueil/";
    private static final String VISALE_URL = "https://www.visale.fr/";

    /** Departments making up Zone 1: Paris and the three inner-ring departments. */
    private static final List<String> ZONE_1_DEPARTMENTS = List.of("75", "92", "93", "94");

    /** Rest of Ile-de-France plus the large cities, which sit in Zone 2. */
    private static final List<String> ZONE_2_DEPARTMENTS = List.of(
            "77", "78", "91", "95",          // outer Ile-de-France
            "69", "13", "59", "31", "33",    // Lyon, Marseille, Lille, Toulouse, Bordeaux
            "06", "44", "67", "34", "35",    // Nice, Nantes, Strasbourg, Montpellier, Rennes
            "38", "76", "51", "21", "30");   // Grenoble, Rouen, Reims, Dijon, Nimes

    private final AidParameters parameters;

    public HousingAidService(AidParameters parameters) {
        this.parameters = parameters;
    }

    // ------------------------------------------------------------------ zoning

    /** Works out the CAF rent-ceiling band from a French postcode. */
    public HousingZone zoneForPostcode(String postcode) {
        if (postcode == null || postcode.length() < 2) {
            return HousingZone.ZONE_3;
        }
        String department = postcode.substring(0, 2);
        if (ZONE_1_DEPARTMENTS.contains(department)) {
            return HousingZone.ZONE_1;
        }
        if (ZONE_2_DEPARTMENTS.contains(department)) {
            return HousingZone.ZONE_2;
        }
        return HousingZone.ZONE_3;
    }

    public String zoneLabel(HousingZone zone) {
        return switch (zone) {
            case ZONE_1 -> "Zone 1 - Paris and the inner suburbs";
            case ZONE_2 -> "Zone 2 - large cities and outer Ile-de-France";
            case ZONE_3 -> "Zone 3 - the rest of France";
        };
    }

    private BigDecimal ceilingFor(HousingZone zone) {
        return switch (zone) {
            case ZONE_1 -> parameters.get(AidParameters.APL_CEILING_ZONE_1);
            case ZONE_2 -> parameters.get(AidParameters.APL_CEILING_ZONE_2);
            case ZONE_3 -> parameters.get(AidParameters.APL_CEILING_ZONE_3);
        };
    }

    // --------------------------------------------------------------- APL estimate

    public AplEstimate estimateForRoom(Room room, User user) {
        boolean shared = room.getRoomType() == RoomType.SHARED;
        BigDecimal charges = Boolean.TRUE.equals(room.getBillsIncluded())
                ? BigDecimal.ZERO
                : (room.getChargesAmount() == null ? BigDecimal.ZERO : room.getChargesAmount());

        HousingZone zone = room.getHousingZone() != null
                ? room.getHousingZone()
                : zoneForPostcode(room.getPostalCode());

        return estimate(room.getPrice(), charges, zone, shared,
                user == null || user.isStudent(),
                user != null && user.isScholarshipHolder(),
                user == null ? null : user.getMonthlyIncome());
    }

    /**
     * The estimate itself.
     *
     * <p>CAF's formula is, in essence: take the rent (capped at a ceiling that depends on
     * where you live), add a flat allowance for bills, then subtract the share you are
     * expected to pay yourself, which grows with your income. What is left is the aid.
     *
     * <p>We return a range rather than one number. The real calculation uses details we do
     * not ask for - savings, exact household make-up, the day of the month you move in -
     * and a confident wrong number would be worse than an honest approximate one.
     */
    public AplEstimate estimate(BigDecimal rent,
                                BigDecimal charges,
                                HousingZone zone,
                                boolean sharedFlat,
                                boolean student,
                                boolean scholarship,
                                BigDecimal monthlyIncome) {

        List<CalculationStep> steps = new ArrayList<>();
        List<String> notes = new ArrayList<>();

        BigDecimal safeRent = rent == null ? BigDecimal.ZERO : rent;
        BigDecimal safeCharges = charges == null ? BigDecimal.ZERO : charges;

        // 1. The rent CAF will actually look at, capped at the ceiling for this zone.
        BigDecimal ceiling = ceilingFor(zone);
        if (sharedFlat) {
            ceiling = ceiling.multiply(parameters.get(AidParameters.APL_SHARED_CEILING_RATE))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        boolean aboveCeiling = safeRent.compareTo(ceiling) > 0;
        BigDecimal rentConsidered = aboveCeiling ? ceiling : safeRent;

        steps.add(new CalculationStep(
                "Your rent",
                euro(safeRent),
                "What the landlord charges each month."));
        steps.add(new CalculationStep(
                "Rent CAF counts",
                euro(rentConsidered),
                aboveCeiling
                        ? "CAF caps the rent it considers at " + euro(ceiling) + " in "
                          + zoneLabel(zone).toLowerCase()
                          + ". Paying more than the cap does not increase your aid."
                        : "Your rent is under the cap for this area, so all of it counts."));

        if (aboveCeiling) {
            notes.add("Your rent is above the ceiling CAF uses, so the aid stops growing "
                    + "beyond " + euro(ceiling) + ". A cheaper flat in the same area would "
                    + "not reduce your aid by much.");
        }

        // 2. The flat allowance CAF adds for bills, whatever you actually pay.
        BigDecimal forfait = parameters.get(AidParameters.APL_CHARGES_FORFAIT);
        steps.add(new CalculationStep(
                "Allowance for bills",
                "+ " + euro(forfait),
                "CAF adds a fixed amount for water, heating and electricity. It is the same "
                        + "figure for everyone living alone, whatever your real bills are."));

        BigDecimal base = rentConsidered.add(forfait);

        // 3. The income the calculation is based on.
        BigDecimal annualIncome = resolveAnnualIncome(student, scholarship, sharedFlat, monthlyIncome, steps, notes);

        // 4. The share you always pay yourself.
        BigDecimal minParticipation = parameters.get(AidParameters.APL_MIN_PARTICIPATION);
        BigDecimal percentShare = base.multiply(BigDecimal.valueOf(0.085)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal participation = percentShare.max(minParticipation);

        steps.add(new CalculationStep(
                "Your own contribution",
                "- " + euro(participation),
                "Everyone pays a minimum share themselves - the greater of 8.5% of the rent "
                        + "plus allowance, or " + euro(minParticipation) + "."));

        // 5. Income above the floor reduces the aid further.
        BigDecimal r0 = parameters.get(AidParameters.APL_R0_SINGLE);
        BigDecimal excessAnnual = annualIncome.subtract(r0).max(BigDecimal.ZERO);
        BigDecimal incomeReduction = excessAnnual
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(0.28))
                .setScale(2, RoundingMode.HALF_UP);

        if (incomeReduction.signum() > 0) {
            steps.add(new CalculationStep(
                    "Reduction for your income",
                    "- " + euro(incomeReduction),
                    "Above " + euro(r0) + " a year, the aid tapers off as income rises."));
        } else {
            steps.add(new CalculationStep(
                    "Reduction for your income",
                    "none",
                    "Your income is below " + euro(r0) + " a year, so nothing is deducted here."));
        }

        BigDecimal aid = base.subtract(participation).subtract(incomeReduction).max(BigDecimal.ZERO);
        aid = aid.setScale(2, RoundingMode.HALF_UP);

        // A deliberate +/- 15% band, floored sensibly, rather than false precision.
        BigDecimal low = aid.multiply(BigDecimal.valueOf(0.85)).setScale(0, RoundingMode.DOWN);
        BigDecimal high = aid.multiply(BigDecimal.valueOf(1.15)).setScale(0, RoundingMode.UP);
        boolean eligible = aid.compareTo(BigDecimal.TEN) > 0;

        BigDecimal effectiveCost = safeRent.add(safeCharges).subtract(aid).max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        notes.add("This is an estimate, not a decision. Only CAF can tell you the real "
                + "amount, and you must apply - the money is never sent automatically.");
        notes.add("Apply in the first month you live there. CAF pays from the month after "
                + "you apply and does not backdate, so a late application is money lost.");
        if (student) {
            notes.add("You can claim as a student on a residence permit. Housing aid is not "
                    + "a benefit that affects your visa or your right to stay.");
        }

        return new AplEstimate(
                eligible,
                safeRent,
                safeCharges,
                zone,
                zoneLabel(zone),
                low,
                high,
                aid,
                effectiveCost,
                rentConsidered,
                aboveCeiling,
                steps,
                notes,
                OFFICIAL_SIMULATOR,
                parameters.lastVerified());
    }

    /**
     * Students with little or no income are not assessed on zero. CAF applies a fixed
     * assumed income instead, which is lower if you hold a scholarship or share a flat.
     */
    private BigDecimal resolveAnnualIncome(boolean student,
                                           boolean scholarship,
                                           boolean sharedFlat,
                                           BigDecimal monthlyIncome,
                                           List<CalculationStep> steps,
                                           List<String> notes) {

        BigDecimal declaredAnnual = monthlyIncome == null
                ? BigDecimal.ZERO
                : monthlyIncome.multiply(BigDecimal.valueOf(12));

        if (!student) {
            steps.add(new CalculationStep(
                    "Income used",
                    euro(declaredAnnual) + " a year",
                    "Based on the monthly income on your profile."));
            return declaredAnnual;
        }

        String key = sharedFlat
                ? (scholarship ? AidParameters.APL_STUDENT_INCOME_SHARED_SCHOLARSHIP
                               : AidParameters.APL_STUDENT_INCOME_SHARED)
                : (scholarship ? AidParameters.APL_STUDENT_INCOME_SCHOLARSHIP
                               : AidParameters.APL_STUDENT_INCOME_STANDARD);

        BigDecimal forfaitIncome = parameters.get(key);
        BigDecimal used = forfaitIncome.max(declaredAnnual);

        steps.add(new CalculationStep(
                "Income used",
                euro(used) + " a year",
                "Students are assessed on a fixed assumed income of " + euro(forfaitIncome)
                        + (scholarship ? " (lower because you hold a scholarship)" : "")
                        + (sharedFlat ? " for a shared flat" : "")
                        + ", even if you earn nothing."));

        if (scholarship) {
            notes.add("Holding a CROUS scholarship lowers the income CAF assumes, which "
                    + "increases your aid. Make sure you declare it.");
        }
        return used;
    }

    // ------------------------------------------------------------------- Visale

    /** Checks the free state guarantor against this person and this rent. */
    public VisaleCheck checkVisale(BigDecimal rent,
                                   BigDecimal charges,
                                   String postcode,
                                   boolean student,
                                   Integer age) {

        List<String> passed = new ArrayList<>();
        List<String> blockers = new ArrayList<>();

        BigDecimal rentInclusive = (rent == null ? BigDecimal.ZERO : rent)
                .add(charges == null ? BigDecimal.ZERO : charges);

        boolean ileDeFrance = isIleDeFrance(postcode);
        BigDecimal ceiling = student
                ? parameters.get(ileDeFrance ? AidParameters.VISALE_STUDENT_IDF
                                             : AidParameters.VISALE_STUDENT_OTHER)
                : parameters.get(ileDeFrance ? AidParameters.VISALE_WORKER_IDF
                                             : AidParameters.VISALE_WORKER_OTHER);

        if (rentInclusive.compareTo(ceiling) <= 0) {
            passed.add("The rent of " + euro(rentInclusive) + " including charges is within the "
                    + euro(ceiling) + " limit for " + (student ? "students" : "workers")
                    + (ileDeFrance ? " in Ile-de-France." : " outside Ile-de-France."));
        } else {
            blockers.add("The rent including charges is " + euro(rentInclusive) + ", above the "
                    + euro(ceiling) + " limit. Visale will not cover this flat.");
        }

        int minAge = parameters.getInt(AidParameters.VISALE_MIN_AGE);
        int maxAge = parameters.getInt(AidParameters.VISALE_MAX_AGE);

        if (age == null) {
            blockers.add("Add your date of birth to your profile so we can check the age limit ("
                    + minAge + " to " + maxAge + ").");
        } else if (age >= minAge && age <= maxAge) {
            passed.add("You are " + age + ", within the " + minAge + " to " + maxAge + " age range.");
        } else {
            blockers.add("Visale is for people aged " + minAge + " to " + maxAge
                    + ", and you are " + age + ".");
        }

        if (student) {
            passed.add("Students qualify regardless of income, which is the whole point of "
                    + "the scheme.");
        }
        passed.add("Your nationality does not matter. Foreign nationals qualify on the same "
                + "terms, with a valid residence permit or student visa.");

        return new VisaleCheck(
                blockers.isEmpty(),
                passed,
                blockers,
                ceiling,
                rentInclusive,
                "Visale is a free guarantor provided by the French state. If you cannot pay "
                        + "your rent, Action Logement pays the landlord and you repay them later.",
                "Most French landlords insist on a guarantor who lives and earns in France. "
                        + "If your family is abroad, you usually cannot provide one, and this is "
                        + "the single most common reason international students are refused a "
                        + "flat. Visale solves exactly that, and it costs nothing.",
                List.of(
                        "Go to visale.fr and create an account. Do this before you start viewing "
                                + "flats - you do not need to have chosen one yet.",
                        "Upload your passport or residence permit, your student enrolment letter, "
                                + "and proof of any income.",
                        "You get a 'visa certificate' back, usually within two working days.",
                        "Show that certificate to landlords. It proves you have a guarantor and "
                                + "makes your application competitive.",
                        "Once you sign a lease, the landlord confirms it on the Visale site."),
                VISALE_URL);
    }

    private boolean isIleDeFrance(String postcode) {
        if (postcode == null || postcode.length() < 2) {
            return false;
        }
        String department = postcode.substring(0, 2);
        return List.of("75", "77", "78", "91", "92", "93", "94", "95").contains(department);
    }

    // ---------------------------------------------------------------- summary

    /** The one-glance answer on a listing card: what this flat really costs you. */
    public AffordabilitySummary summarise(Room room, User user) {
        AplEstimate apl = estimateForRoom(room, user);

        Integer age = user == null ? null : ageOf(user);
        boolean student = user == null || user.isStudent();
        VisaleCheck visale = checkVisale(room.getPrice(), apl.monthlyCharges(),
                room.getPostalCode(), student, age);

        BigDecimal deposit = room.getDeposit() == null ? BigDecimal.ZERO : room.getDeposit();
        BigDecimal firstMonth = room.getPrice() == null ? BigDecimal.ZERO : room.getPrice();
        BigDecimal upfront = deposit.add(firstMonth).add(apl.monthlyCharges());

        List<CalculationStep> upfrontSteps = List.of(
                new CalculationStep("First month's rent", euro(firstMonth),
                        "Paid when you sign, before you move in."),
                new CalculationStep("Deposit", euro(deposit),
                        "Held until you leave and returned if there is no damage. By law it "
                                + "cannot exceed one month's rent for a furnished-exempt lease."),
                new CalculationStep("Charges", euro(apl.monthlyCharges()),
                        "Bills, if they are not already included in the rent."));

        String sentence = apl.likelyEligible()
                ? "Advertised at " + euro(room.getPrice()) + " a month, but housing aid should "
                  + "cover roughly " + euro(apl.estimateMid().setScale(0, RoundingMode.HALF_UP))
                  + ", leaving you about "
                  + euro(apl.effectiveMonthlyCost().setScale(0, RoundingMode.HALF_UP)) + "."
                : "Housing aid is unlikely to help much here, so budget the full "
                  + euro(room.totalMonthlyCost()) + " a month.";

        return new AffordabilitySummary(
                room.getId(),
                room.getPrice(),
                apl.monthlyCharges(),
                room.totalMonthlyCost(),
                apl.estimateMid(),
                apl.effectiveMonthlyCost(),
                visale.eligible(),
                upfront,
                upfrontSteps,
                sentence);
    }

    public Integer ageOf(User user) {
        if (user == null || user.getDateOfBirth() == null) {
            return null;
        }
        return Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();
    }

    private static String euro(BigDecimal value) {
        if (value == null) {
            return "EUR 0";
        }
        return "EUR " + value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
