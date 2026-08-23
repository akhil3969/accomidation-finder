package com.accommodationfinder.exception;

/** Thrown when the request is syntactically fine but semantically invalid. Maps to HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
