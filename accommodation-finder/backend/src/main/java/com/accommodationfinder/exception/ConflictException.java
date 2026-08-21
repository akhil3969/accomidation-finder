package com.accommodationfinder.exception;

/** Thrown when an action clashes with existing state (duplicate email, overlapping booking). Maps to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
