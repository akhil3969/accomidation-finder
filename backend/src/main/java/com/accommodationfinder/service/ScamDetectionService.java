package com.accommodationfinder.service;

import com.accommodationfinder.dto.NewcomerDtos.SafetyReport;
import com.accommodationfinder.model.Room;
import com.accommodationfinder.model.User;
import com.accommodationfinder.model.VerificationStatus;
import com.accommodationfinder.repository.ReportRepository;
import com.accommodationfinder.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Scores how likely a listing is to be a scam, and says why in plain words.
 *
 * <p>Rental fraud is the thing that actually hurts people in this market, and newcomers are
 * the target: they cannot visit before arriving, they do not know what a normal price looks
 * like, and they are desperate enough to wire a deposit to a stranger. The pattern almost
 * never varies - a flat well below market rate, an owner who is conveniently abroad, and a
 * request to transfer money before anyone has seen a key.
 *
 * <p>This does not block anything on its own. It raises the listing to a moderator and warns
 * the reader, because an automated judgement that quietly hides honest landlords would be
 * its own kind of harm.
 */
@Service
public class ScamDetectionService {

    /** Phrases that show up again and again in advance-fee rental fraud. */
    private static final List<String> SUSPICIOUS_PHRASES = List.of(
            "western union", "moneygram", "money transfer", "wire transfer",
            "send money", "deposit before", "pay before", "transfer the deposit",
            "keys by post", "keys by mail", "send the keys", "courier the keys",
            "i am abroad", "i am currently abroad", "out of the country",
            "missionary", "working overseas", "cannot show", "cannot visit",
            "no viewing", "without visiting", "trust me", "god bless",
            "urgent", "act fast", "first come first served",
            "whatsapp only", "contact me on whatsapp", "telegram");

    /** Contact details in the description usually mean "let us talk off-platform". */
    private static final List<String> OFF_PLATFORM = List.of(
            "@gmail", "@yahoo", "@hotmail", "@outlook", "whatsapp", "telegram", "wechat");

    private final RoomRepository roomRepository;
    private final ReportRepository reportRepository;

    public ScamDetectionService(RoomRepository roomRepository, ReportRepository reportRepository) {
        this.roomRepository = roomRepository;
        this.reportRepository = reportRepository;
    }

    /**
     * Recomputes the risk score for a listing. Called whenever a listing is created or
     * edited, and by the admin panel on demand.
     */
    @Transactional
    public Room rescore(Room room) {
        Set<String> flags = new LinkedHashSet<>();
        int score = 0;

        String haystack = ((room.getTitle() == null ? "" : room.getTitle()) + " "
                + (room.getDescription() == null ? "" : room.getDescription()))
                .toLowerCase(Locale.ROOT);

        // --- what the advert says -------------------------------------------
        for (String phrase : SUSPICIOUS_PHRASES) {
            if (haystack.contains(phrase)) {
                flags.add("Advert uses a phrase common in rental fraud: \"" + phrase + "\"");
                score += 22;
                break;
            }
        }
        for (String contact : OFF_PLATFORM) {
            if (haystack.contains(contact)) {
                flags.add("Advert pushes you to contact the landlord outside the platform");
                score += 15;
                break;
            }
        }
        if (room.getDescription() == null || room.getDescription().trim().length() < 60) {
            flags.add("Barely any description - real landlords usually write more");
            score += 10;
        }

        // --- price against the local market ---------------------------------
        Double average = roomRepository.averagePriceForCity(room.getCity());
        if (average != null && average > 0 && room.getPrice() != null) {
            BigDecimal marketAverage = BigDecimal.valueOf(average);
            BigDecimal ratio = room.getPrice().divide(marketAverage, 2, java.math.RoundingMode.HALF_UP);
            if (ratio.doubleValue() < 0.5) {
                flags.add("Priced less than half the average for " + room.getCity()
                        + " - the classic bait");
                score += 30;
            } else if (ratio.doubleValue() < 0.7) {
                flags.add("Noticeably cheaper than similar places in " + room.getCity());
                score += 12;
            }
        }

        // --- the photos ------------------------------------------------------
        if (room.getImages() == null || room.getImages().isEmpty()) {
            flags.add("No photos at all");
            score += 18;
        } else if (room.getImages().size() == 1) {
            flags.add("Only one photo");
            score += 6;
        }

        // --- who is behind it ------------------------------------------------
        User landlord = room.getLandlord();
        if (landlord != null) {
            if (landlord.getVerificationStatus() != VerificationStatus.VERIFIED) {
                flags.add("Landlord has not verified their identity");
                score += 12;
            }
            if (landlord.getCreatedAt() != null
                    && landlord.getCreatedAt().isAfter(LocalDateTime.now().minusDays(3))) {
                flags.add("Account was created in the last three days");
                score += 10;
            }
            if (landlord.getPhone() == null || landlord.getPhone().isBlank()) {
                flags.add("No phone number on the landlord's profile");
                score += 5;
            }
        }

        // --- what other people have said -------------------------------------
        long reports = reportRepository.countByRoomId(room.getId() == null ? -1L : room.getId());
        if (reports >= 3) {
            flags.add(reports + " people have reported this listing");
            score += 40;
        } else if (reports > 0) {
            flags.add(reports + " report" + (reports == 1 ? "" : "s") + " from other users");
            score += 15;
        }

        room.setRiskScore(Math.min(100, score));
        room.setRiskFlags(flags);
        return room;
    }

    /** The reader-facing version: warnings, reassurances, and the rules that keep you safe. */
    @Transactional(readOnly = true)
    public SafetyReport reportFor(Room room) {
        int score = room.getRiskScore() == null ? 0 : room.getRiskScore();
        List<String> warnings = new ArrayList<>(
                room.getRiskFlags() == null ? Set.<String>of() : room.getRiskFlags());
        List<String> reassurances = new ArrayList<>();

        User landlord = room.getLandlord();
        boolean verified = landlord != null
                && landlord.getVerificationStatus() == VerificationStatus.VERIFIED;

        if (verified) {
            reassurances.add("This landlord's identity has been checked by our team.");
        }
        if (room.isVerified()) {
            reassurances.add("We have confirmed this address and these photos.");
        }
        if (room.getImages() != null && room.getImages().size() >= 3) {
            reassurances.add("Several photos, which makes a copied advert less likely.");
        }
        if (landlord != null && landlord.getCreatedAt() != null
                && landlord.getCreatedAt().isBefore(LocalDateTime.now().minusMonths(3))) {
            reassurances.add("The landlord has had an account here for several months.");
        }

        String level;
        if (score >= 55) {
            level = "high";
        } else if (score >= 25) {
            level = "caution";
        } else {
            level = "normal";
        }

        return new SafetyReport(
                room.getId(),
                score,
                level,
                warnings,
                reassurances,
                verified,
                List.of(
                        "Never send money before you have seen the flat, in person or on a live "
                                + "video call where the person shows you around.",
                        "No honest landlord in France asks for a deposit by Western Union, "
                                + "MoneyGram or crypto. A bank transfer to a French IBAN, after "
                                + "signing a lease, is normal.",
                        "You should receive a written lease (bail) and a signed inventory "
                                + "(etat des lieux). Without them you have no legal protection.",
                        "The deposit is capped by law: one month's rent for an unfurnished flat, "
                                + "two for a furnished one. Anyone asking for more is breaking the rules.",
                        "Agency fees are capped too, and a private landlord cannot charge them "
                                + "at all.",
                        "If it is far cheaper than everything else nearby, there is a reason, and "
                                + "it is rarely a good one."));
    }
}
