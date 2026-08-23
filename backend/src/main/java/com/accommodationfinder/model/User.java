package com.accommodationfinder.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true)
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** BCrypt hash - never exposed through the API. */
    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.TENANT;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(length = 500)
    private String bio;

    @Column(nullable = false)
    private boolean enabled = true;

    // ---------------------------------------------------------- newcomer profile
    // Filled in by the arrival wizard. Everything here exists to answer one question
    // for the user: what will this flat actually cost me, and can I even apply for it?

    /** Country the person is arriving from, e.g. "India". Drives the visa/EU guidance. */
    @Column(length = 80)
    private String nationality;

    /** True when the nationality is inside the EU/EEA/Switzerland - far fewer documents. */
    @Column(name = "eu_national")
    private Boolean euNational;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** Visale is only open to people under 31, so age is a real eligibility input. */
    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(name = "is_student", nullable = false)
    private boolean student = true;

    /** Boursier. A CROUS scholarship lowers the income CAF assumes you have. */
    @Column(name = "scholarship_holder", nullable = false)
    private boolean scholarshipHolder = false;

    @Column(length = 150)
    private String university;

    /** Where they study, so we can show real travel time from each listing. */
    @Column(name = "university_lat", precision = 9, scale = 6)
    private BigDecimal universityLatitude;

    @Column(name = "university_lon", precision = 9, scale = 6)
    private BigDecimal universityLongitude;

    /** Net monthly income in euros. Null means "none yet", which is normal on arrival. */
    @Column(name = "monthly_income", precision = 10, scale = 2)
    private BigDecimal monthlyIncome;

    /** Almost always false for someone who has just arrived - which is why Visale exists. */
    @Column(name = "has_french_guarantor", nullable = false)
    private boolean hasFrenchGuarantor = false;

    @Column(name = "profile_completed", nullable = false)
    private boolean profileCompleted = false;

    // ------------------------------------------------------ landlord verification

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private VerificationStatus verificationStatus = VerificationStatus.NOT_SUBMITTED;

    @Column(name = "verification_note", length = 500)
    private String verificationNote;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public User() {
    }

    public User(String name, String email, String password, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public Boolean getEuNational() { return euNational; }
    public void setEuNational(Boolean euNational) { this.euNational = euNational; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public LocalDate getArrivalDate() { return arrivalDate; }
    public void setArrivalDate(LocalDate arrivalDate) { this.arrivalDate = arrivalDate; }

    public boolean isStudent() { return student; }
    public void setStudent(boolean student) { this.student = student; }

    public boolean isScholarshipHolder() { return scholarshipHolder; }
    public void setScholarshipHolder(boolean scholarshipHolder) { this.scholarshipHolder = scholarshipHolder; }

    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }

    public BigDecimal getUniversityLatitude() { return universityLatitude; }
    public void setUniversityLatitude(BigDecimal universityLatitude) { this.universityLatitude = universityLatitude; }

    public BigDecimal getUniversityLongitude() { return universityLongitude; }
    public void setUniversityLongitude(BigDecimal universityLongitude) { this.universityLongitude = universityLongitude; }

    public BigDecimal getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(BigDecimal monthlyIncome) { this.monthlyIncome = monthlyIncome; }

    public boolean isHasFrenchGuarantor() { return hasFrenchGuarantor; }
    public void setHasFrenchGuarantor(boolean hasFrenchGuarantor) { this.hasFrenchGuarantor = hasFrenchGuarantor; }

    public boolean isProfileCompleted() { return profileCompleted; }
    public void setProfileCompleted(boolean profileCompleted) { this.profileCompleted = profileCompleted; }

    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getVerificationNote() { return verificationNote; }
    public void setVerificationNote(String verificationNote) { this.verificationNote = verificationNote; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
