package com.accommodationfinder.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "rooms", indexes = {
        @Index(name = "idx_rooms_city", columnList = "city"),
        @Index(name = "idx_rooms_price", columnList = "price"),
        @Index(name = "idx_rooms_available", columnList = "available")
})
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 255)
    private String address;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 100)
    private String country = "France";

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Optional one-off charges shown separately in the UI. */
    @Column(name = "deposit", precision = 10, scale = 2)
    private BigDecimal deposit;

    @Column(name = "size_sqm")
    private Integer sizeSqm;

    private Integer bedrooms = 1;

    private Integer bathrooms = 1;

    @Column(name = "max_guests")
    private Integer maxGuests = 1;

    @Column(nullable = false)
    private Boolean available = Boolean.TRUE;

    private Boolean furnished = Boolean.TRUE;

    @Column(name = "bills_included")
    private Boolean billsIncluded = Boolean.FALSE;

    @Column(name = "available_from")
    private LocalDate availableFrom;

    @Column(name = "min_stay_months")
    private Integer minStayMonths = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 20)
    private RoomType roomType = RoomType.STUDIO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "room_images", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "url", length = 500)
    @OrderColumn(name = "position")
    private List<String> images = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "room_amenities", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "amenity", length = 60)
    private Set<String> amenities = new LinkedHashSet<>();

    /** INSEE commune code, filled in by geocoding. Identifies the commune unambiguously. */
    @Column(name = "insee_code", length = 10)
    private String inseeCode;

    /** Which CAF rent-ceiling band this address falls in. Derived from the postcode. */
    @Enumerated(EnumType.STRING)
    @Column(name = "housing_zone", length = 10)
    private HousingZone housingZone;

    /** Monthly charges on top of the rent, when they are not included. */
    @Column(name = "charges_amount", precision = 10, scale = 2)
    private BigDecimal chargesAmount;

    /** Set by a moderator once the landlord's identity and the address check out. */
    @Column(nullable = false)
    private boolean verified = false;

    /** Hidden listings stay in the database for the audit trail but leave the search. */
    @Column(nullable = false)
    private boolean hidden = false;

    /** 0-100, computed by ScamDetectionService. Higher means more suspicious. */
    @Column(name = "risk_score")
    private Integer riskScore = 0;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "room_risk_flags", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "flag", length = 80)
    private Set<String> riskFlags = new LinkedHashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (availableFrom == null) {
            availableFrom = LocalDate.now();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getDeposit() { return deposit; }
    public void setDeposit(BigDecimal deposit) { this.deposit = deposit; }

    public Integer getSizeSqm() { return sizeSqm; }
    public void setSizeSqm(Integer sizeSqm) { this.sizeSqm = sizeSqm; }

    public Integer getBedrooms() { return bedrooms; }
    public void setBedrooms(Integer bedrooms) { this.bedrooms = bedrooms; }

    public Integer getBathrooms() { return bathrooms; }
    public void setBathrooms(Integer bathrooms) { this.bathrooms = bathrooms; }

    public Integer getMaxGuests() { return maxGuests; }
    public void setMaxGuests(Integer maxGuests) { this.maxGuests = maxGuests; }

    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }

    public Boolean getFurnished() { return furnished; }
    public void setFurnished(Boolean furnished) { this.furnished = furnished; }

    public Boolean getBillsIncluded() { return billsIncluded; }
    public void setBillsIncluded(Boolean billsIncluded) { this.billsIncluded = billsIncluded; }

    public LocalDate getAvailableFrom() { return availableFrom; }
    public void setAvailableFrom(LocalDate availableFrom) { this.availableFrom = availableFrom; }

    public Integer getMinStayMonths() { return minStayMonths; }
    public void setMinStayMonths(Integer minStayMonths) { this.minStayMonths = minStayMonths; }

    public RoomType getRoomType() { return roomType; }
    public void setRoomType(RoomType roomType) { this.roomType = roomType; }

    public User getLandlord() { return landlord; }
    public void setLandlord(User landlord) { this.landlord = landlord; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) {
        this.images = images == null ? new ArrayList<>() : new ArrayList<>(images);
    }

    public Set<String> getAmenities() { return amenities; }
    public void setAmenities(Set<String> amenities) {
        this.amenities = amenities == null ? new LinkedHashSet<>() : new LinkedHashSet<>(amenities);
    }

    public String getInseeCode() { return inseeCode; }
    public void setInseeCode(String inseeCode) { this.inseeCode = inseeCode; }

    public HousingZone getHousingZone() { return housingZone; }
    public void setHousingZone(HousingZone housingZone) { this.housingZone = housingZone; }

    public BigDecimal getChargesAmount() { return chargesAmount; }
    public void setChargesAmount(BigDecimal chargesAmount) { this.chargesAmount = chargesAmount; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public Set<String> getRiskFlags() { return riskFlags; }
    public void setRiskFlags(Set<String> riskFlags) {
        this.riskFlags = riskFlags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(riskFlags);
    }

    /** Total monthly outlay: rent plus charges when they are billed separately. */
    public BigDecimal totalMonthlyCost() {
        BigDecimal total = price == null ? BigDecimal.ZERO : price;
        if (!Boolean.TRUE.equals(billsIncluded) && chargesAmount != null) {
            total = total.add(chargesAmount);
        }
        return total;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
