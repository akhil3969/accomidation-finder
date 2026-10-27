package com.accommodationfinder.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.rate-limit.enabled=true",
        "app.rate-limit.login.limit=2",
        "app.rate-limit.login.window=1m"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitTests {

    @Autowired private MockMvc mockMvc;

    @Test
    void loginGuessingIsCutOffWith429AndRetryAfter() throws Exception {
        String body = "{\"email\":\"victim@test.fr\",\"password\":\"guess\"}";
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void pageSizeIsCappedAt100() throws Exception {
        mockMvc.perform(get("/api/rooms").param("size", "1000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }
}
