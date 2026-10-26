package com.accommodationfinder.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** The test profile, like prod, has the H2 console switched off. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHardeningTests {

    @Autowired private MockMvc mockMvc;

    @Test
    void h2ConsoleIsNotPublicWhenItIsDisabled() throws Exception {
        mockMvc.perform(get("/h2-console/"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pagesMayNotBeFramedWhenTheH2ConsoleIsOff() throws Exception {
        mockMvc.perform(get("/api/rooms"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }

    @Test
    void malformedJsonDoesNotEchoParserInternals() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", not(containsString("com.fasterxml"))))
                .andExpect(jsonPath("$.message", not(containsString("LoginRequest"))));
    }
}
