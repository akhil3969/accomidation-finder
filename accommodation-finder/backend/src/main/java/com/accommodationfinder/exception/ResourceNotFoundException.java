package com.accommodationfinder.exception;

/** Thrown when an entity referenced by id does not exist. Maps to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String entity, Object id) {
        super(entity + " not found with id " + id);
    }
}
