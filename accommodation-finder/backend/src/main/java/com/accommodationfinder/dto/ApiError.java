package com.accommodationfinder.dto;

import java.time.LocalDateTime;
import java.util.Map;

/** Uniform error body returned for every non-2xx response. */
public record ApiError(
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors,
        LocalDateTime timestamp
) {
    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, null, LocalDateTime.now());
    }

    public static ApiError validation(String message, String path, Map<String, String> fieldErrors) {
        return new ApiError(400, "Bad Request", message, path, fieldErrors, LocalDateTime.now());
    }
}
