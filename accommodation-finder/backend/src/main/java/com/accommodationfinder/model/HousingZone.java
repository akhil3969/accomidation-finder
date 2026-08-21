package com.accommodationfinder.model;

/**
 * The three zones the CAF uses to cap how much rent housing aid will consider.
 * Determined from the postcode of the listing.
 */
public enum HousingZone {
    /** Paris and the inner ring. */
    ZONE_1,
    /** Cities over 100,000 people, and the outer Ile-de-France ring. */
    ZONE_2,
    /** Everywhere else. */
    ZONE_3
}
