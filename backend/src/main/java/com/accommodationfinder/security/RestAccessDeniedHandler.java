package com.accommodationfinder.security;

import com.accommodationfinder.dto.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns a JSON 403 when a signed-in user reaches something they are not
 * allowed to see.
 *
 * <p>Spring's default handler calls {@code sendError}, which forwards the
 * request to {@code /error}. That internal dispatch runs through the security
 * filter chain a second time carrying no authentication, so the entry point
 * treats it as anonymous and replaces the 403 with a 401 - the response says
 * "log in" to someone who is already logged in.
 *
 * <p>That mattered beyond tidiness: the browser client clears the stored token
 * on any 401, so a tenant opening an admin-only URL was silently signed out
 * instead of simply being refused. Writing the body here avoids the forward
 * entirely, exactly as {@link RestAuthenticationEntryPoint} does.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = ApiError.of(403, "Forbidden",
                "Your account does not have access to this resource", request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
