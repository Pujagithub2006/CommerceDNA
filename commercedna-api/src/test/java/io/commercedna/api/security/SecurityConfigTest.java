package io.commercedna.api.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicEndpoints_shouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
        
        mockMvc.perform(get("/.well-known/commercedna.json"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoints_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/some-id"))
                .andExpect(status().isUnauthorized());
        
        mockMvc.perform(get("/api/v1/analytics/overview"))
                .andExpect(status().isUnauthorized());
    }
}