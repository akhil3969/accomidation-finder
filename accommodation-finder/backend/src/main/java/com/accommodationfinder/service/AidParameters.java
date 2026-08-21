package com.accommodationfinder.service;

import com.accommodationfinder.model.AidParameter;
import com.accommodationfinder.repository.AidParameterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads the official housing-aid figures from the database, falling back to the values
 * verified when this was written.
 *
 * <p>Every one of these numbers is set by decree and changes at least yearly. Hard-coding
 * them would mean the estimator quietly goes wrong twelve months from now and nobody
 * notices. Storing them - with the source and the date they were last checked - lets an
 * admin fix them in a minute, and lets the UI tell the user how fresh they are.
 */
@Service
public class AidParameters {

    private static final Logger log = LoggerFactory.getLogger(AidParameters.class);

    // --- APL, in force from 1 October 2025 (source: aide-sociale.fr, simulez.fr) ---
    public static final String APL_CEILING_ZONE_1 = "apl.ceiling.zone1";
    public static final String APL_CEILING_ZONE_2 = "apl.ceiling.zone2";
    public static final String APL_CEILING_ZONE_3 = "apl.ceiling.zone3";
    public static final String APL_CHARGES_FORFAIT = "apl.charges.forfait";
    public static final String APL_MIN_PARTICIPATION = "apl.participation.min";
    public static final String APL_R0_SINGLE = "apl.r0.single";
    public static final String APL_STUDENT_INCOME_STANDARD = "apl.student.income.standard";
    public static final String APL_STUDENT_INCOME_SCHOLARSHIP = "apl.student.income.scholarship";
    public static final String APL_STUDENT_INCOME_SHARED = "apl.student.income.shared";
    public static final String APL_STUDENT_INCOME_SHARED_SCHOLARSHIP = "apl.student.income.sharedScholarship";
    public static final String APL_SHARED_CEILING_RATE = "apl.shared.ceilingRate";

    // --- Visale, ceilings raised 6 January 2026 (source: Action Logement) ---
    public static final String VISALE_STUDENT_IDF = "visale.student.idf";
    public static final String VISALE_STUDENT_OTHER = "visale.student.other";
    public static final String VISALE_WORKER_IDF = "visale.worker.idf";
    public static final String VISALE_WORKER_BIG_CITY = "visale.worker.bigCity";
    public static final String VISALE_WORKER_OTHER = "visale.worker.other";
    public static final String VISALE_MAX_AGE = "visale.age.max";
    public static final String VISALE_MIN_AGE = "visale.age.min";

    private static final Map<String, Object[]> DEFAULTS = new LinkedHashMap<>();

    static {
        // key -> { value, label, source, effectiveFrom }
        String aplSource = "https://www.aide-sociale.fr/montant-apl/";
        String aplFrom = "2025-10-01";
        DEFAULTS.put(APL_CEILING_ZONE_1, new Object[]{"333.14",
                "Maximum rent CAF will consider - Zone 1 (Paris and inner ring)", aplSource, aplFrom});
        DEFAULTS.put(APL_CEILING_ZONE_2, new Object[]{"290.34",
                "Maximum rent CAF will consider - Zone 2 (large cities)", aplSource, aplFrom});
        DEFAULTS.put(APL_CEILING_ZONE_3, new Object[]{"272.12",
                "Maximum rent CAF will consider - Zone 3 (rest of France)", aplSource, aplFrom});
        DEFAULTS.put(APL_CHARGES_FORFAIT, new Object[]{"60.59",
                "Flat allowance CAF adds for bills, single person", aplSource, aplFrom});
        DEFAULTS.put(APL_MIN_PARTICIPATION, new Object[]{"39.56",
                "Minimum you always pay yourself", aplSource, aplFrom});
        DEFAULTS.put(APL_R0_SINGLE, new Object[]{"5235",
                "Yearly income below which you get the maximum, single person", aplSource, aplFrom});
        DEFAULTS.put(APL_STUDENT_INCOME_STANDARD, new Object[]{"8600",
                "Income CAF assumes for a student with no scholarship, own place", aplSource, "2026-01-01"});
        DEFAULTS.put(APL_STUDENT_INCOME_SCHOLARSHIP, new Object[]{"6900",
                "Income CAF assumes for a scholarship student, own place", aplSource, "2026-01-01"});
        DEFAULTS.put(APL_STUDENT_INCOME_SHARED, new Object[]{"6600",
                "Income CAF assumes for a student with no scholarship, shared flat", aplSource, "2026-01-01"});
        DEFAULTS.put(APL_STUDENT_INCOME_SHARED_SCHOLARSHIP, new Object[]{"5400",
                "Income CAF assumes for a scholarship student, shared flat", aplSource, "2026-01-01"});
        DEFAULTS.put(APL_SHARED_CEILING_RATE, new Object[]{"0.75",
                "Share of the rent ceiling applied in a shared flat", aplSource, aplFrom});

        String visaleSource = "https://www.visale.fr/";
        String visaleFrom = "2026-01-06";
        DEFAULTS.put(VISALE_STUDENT_IDF, new Object[]{"1000",
                "Maximum rent Visale covers for students in Ile-de-France", visaleSource, visaleFrom});
        DEFAULTS.put(VISALE_STUDENT_OTHER, new Object[]{"680",
                "Maximum rent Visale covers for students elsewhere in France", visaleSource, visaleFrom});
        DEFAULTS.put(VISALE_WORKER_IDF, new Object[]{"1940",
                "Maximum rent Visale covers for workers in Ile-de-France", visaleSource, visaleFrom});
        DEFAULTS.put(VISALE_WORKER_BIG_CITY, new Object[]{"1575",
                "Maximum rent Visale covers for workers in cities over 100,000", visaleSource, visaleFrom});
        DEFAULTS.put(VISALE_WORKER_OTHER, new Object[]{"1365",
                "Maximum rent Visale covers for workers in other communes", visaleSource, visaleFrom});
        DEFAULTS.put(VISALE_MIN_AGE, new Object[]{"18", "Youngest age Visale accepts", visaleSource, visaleFrom});
        DEFAULTS.put(VISALE_MAX_AGE, new Object[]{"30", "Oldest age Visale accepts", visaleSource, visaleFrom});
    }

    private final AidParameterRepository repository;

    public AidParameters(AidParameterRepository repository) {
        this.repository = repository;
    }

    /** Writes any parameter missing from the database. Safe to call on every startup. */
    @Transactional
    public void seedMissing() {
        int created = 0;
        for (Map.Entry<String, Object[]> entry : DEFAULTS.entrySet()) {
            if (repository.findByKey(entry.getKey()).isPresent()) {
                continue;
            }
            Object[] spec = entry.getValue();
            repository.save(new AidParameter(
                    entry.getKey(),
                    new BigDecimal((String) spec[0]),
                    (String) spec[1],
                    (String) spec[2],
                    LocalDate.parse((String) spec[3])));
            created++;
        }
        if (created > 0) {
            log.info("Seeded {} housing-aid parameters", created);
        }
    }

    @Transactional(readOnly = true)
    public BigDecimal get(String key) {
        return repository.findByKey(key)
                .map(AidParameter::getValue)
                .orElseGet(() -> fallback(key));
    }

    public double getDouble(String key) {
        return get(key).doubleValue();
    }

    public int getInt(String key) {
        return get(key).intValue();
    }

    /** The date the figures were last confirmed against the official source. */
    @Transactional(readOnly = true)
    public String lastVerified() {
        return repository.findByKey(APL_CEILING_ZONE_1)
                .map(parameter -> parameter.getLastVerified() == null
                        ? "unknown"
                        : parameter.getLastVerified().toLocalDate().toString())
                .orElse("unknown");
    }

    private BigDecimal fallback(String key) {
        Object[] spec = DEFAULTS.get(key);
        if (spec == null) {
            log.warn("No housing-aid parameter or default for key {}", key);
            return BigDecimal.ZERO;
        }
        return new BigDecimal((String) spec[0]);
    }
}
