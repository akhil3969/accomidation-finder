package com.accommodationfinder.model;

/** Account type. Drives authorisation rules across the API. */
public enum Role {
    /** Looks for and books accommodation. */
    TENANT,
    /** Publishes and manages listings. */
    LANDLORD,
    /** Full moderation rights. */
    ADMIN
}
