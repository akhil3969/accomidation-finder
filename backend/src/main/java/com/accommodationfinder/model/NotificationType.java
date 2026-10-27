package com.accommodationfinder.model;

/** What a notification is about. Drives the icon in the bell and the email subject line. */
public enum NotificationType {
    BOOKING_REQUESTED,
    BOOKING_CONFIRMED,
    BOOKING_REJECTED,
    BOOKING_CANCELLED,
    BOOKING_COMPLETED,
    NEW_MESSAGE
}
