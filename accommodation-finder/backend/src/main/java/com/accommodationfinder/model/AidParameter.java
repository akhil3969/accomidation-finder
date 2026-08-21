package com.accommodationfinder.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One official figure used by the housing-aid estimator - a rent ceiling, the charges
 * allowance, a Visale limit.
 *
 * <p>These change every year by decree. Keeping them in a table, with the source they came
 * from and the date they were last checked, means an admin can correct them the week CAF
 * publishes new ones instead of waiting for a code change. The estimator shows the user
 * exactly which values it used.
 */
@Entity
@Table(name = "aid_parameters")
public class AidParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g. {@code apl.ceiling.zone1}, {@code visale.student.idf}. */
    @Column(name = "param_key", nullable = false, unique = true, length = 80)
    private String key;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal value;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(length = 500)
    private String source;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "last_verified")
    private LocalDateTime lastVerified;

    public AidParameter() {
    }

    public AidParameter(String key, BigDecimal value, String label, String source, LocalDate effectiveFrom) {
        this.key = key;
        this.value = value;
        this.label = label;
        this.source = source;
        this.effectiveFrom = effectiveFrom;
        this.lastVerified = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDateTime getLastVerified() { return lastVerified; }
    public void setLastVerified(LocalDateTime lastVerified) { this.lastVerified = lastVerified; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AidParameter other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
