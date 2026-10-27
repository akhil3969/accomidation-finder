package com.accommodationfinder.controller;

import com.accommodationfinder.model.User;
import com.accommodationfinder.repository.UserRepository;
import com.accommodationfinder.service.MailGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Registration consent, email verification and password reset, end to end over HTTP. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthEmailFlowTests {

    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @MockitoBean private MailGateway mailGateway;

    @Test
    void registrationWithoutAcceptingTheTermsIsRefused() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "No Consent", "email", uniqueEmail(),
                                "password", "password123", "acceptTerms", false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.acceptTerms").exists());
    }

    @Test
    void registrationRecordsConsentAndTheEmailedLinkVerifiesTheAddressOnce() throws Exception {
        String email = uniqueEmail();
        String jwt = register(email, "password123");

        User saved = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(saved.getTermsAcceptedAt()).isNotNull();
        assertThat(saved.getTermsVersion()).isEqualTo("2026-10");
        assertThat(saved.isEmailVerified()).isFalse();

        String token = tokenFromEmail(email, "Confirm your email address");
        mockMvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified").value(true));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jwt))
                .andExpect(jsonPath("$.emailVerified").value(true));

        // Single use.
        mockMvc.perform(post("/api/auth/verify-email").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", token))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anUnverifiedAccountCannotBookOrMessage() throws Exception {
        String jwt = register(uniqueEmail(), "password123");
        LocalDate checkIn = LocalDate.now().plusDays(10);

        mockMvc.perform(post("/api/bookings").header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("roomId", 1, "checkIn", checkIn.toString(),
                                "checkOut", checkIn.plusDays(30).toString(), "guests", 1))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("confirm your email")));

        mockMvc.perform(post("/api/messages").header("Authorization", "Bearer " + jwt)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("recipientId", 1, "roomId", 1, "content", "Hello"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void forgotPasswordNeverRevealsWhetherAnAccountExists() throws Exception {
        String nobody = uniqueEmail();
        mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", nobody))))
                .andExpect(status().isNoContent());
        verify(mailGateway, never()).send(eq(nobody), anyString(), anyString());
    }

    @Test
    void aPasswordResetWorksOnceAndSignsOutEveryDevice() throws Exception {
        String email = uniqueEmail();
        String oldJwt = register(email, "password123");

        mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email))))
                .andExpect(status().isNoContent());
        String token = tokenFromEmail(email, "Reset your password");

        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", token, "newPassword", "a-brand-new-password"))))
                .andExpect(status().isNoContent());

        // The token held before the reset - possibly by whoever forced the reset - is dead.
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + oldJwt))
                .andExpect(status().isUnauthorized());

        login(email, "password123").andExpect(status().isUnauthorized());
        login(email, "a-brand-new-password").andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", token, "newPassword", "another-password-1"))))
                .andExpect(status().isBadRequest());

        verify(mailGateway).send(eq(email), eq("Your password was changed"), anyString());
        // Clicking a link sent to the inbox proves the inbox is theirs.
        assertThat(userRepository.findByEmailIgnoreCase(email).orElseThrow().isEmailVerified()).isTrue();
    }

    // ------------------------------------------------------------------ helpers

    private String register(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Flow Tester", "email", email, "password", password,
                                "role", "TENANT", "acceptTerms", true))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password))));
    }

    private String tokenFromEmail(String to, String subject) {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mailGateway, atLeastOnce()).send(eq(to), eq(subject), body.capture());
        Matcher matcher = TOKEN.matcher(body.getValue());
        assertThat(matcher.find()).as("email contains a link with a token").isTrue();
        return matcher.group(1);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static String uniqueEmail() {
        return "flow-" + UUID.randomUUID() + "@test.fr";
    }
}
