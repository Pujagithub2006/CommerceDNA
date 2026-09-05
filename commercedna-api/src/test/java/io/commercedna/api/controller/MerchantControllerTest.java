package io.commercedna.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.commercedna.api.dto.RegisterMerchantRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MerchantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Should successfully register merchant and retrieve DNA manifest")
    void shouldRegisterMerchantAndFetchDna() throws Exception {
        RegisterMerchantRequest request = new RegisterMerchantRequest(
                "titan_watches",
                "Titan Watches Ltd",
                "merchants@titan.co.in",
                "rzp_test_key123",
                "rzp_test_secret456",
                "webhook_sec_789"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(jsonPath("$.merchantCode").value("titan_watches"))
                .andExpect(jsonPath("$.merchantDid").value("did:cdna:merchant:titan_watches"))
                .andExpect(jsonPath("$.publicKeyEd25519").isNotEmpty())
                .andExpect(jsonPath("$.privateKeyEd25519").isNotEmpty())
                .andExpect(jsonPath("$.jwtToken").isNotEmpty())
                .andReturn();

        Map<?, ?> responseMap = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        String merchantId = (String) responseMap.get("merchantId");
        assertThat(merchantId).isNotNull();

        // Verify GET /api/v1/merchants/{id}
        mockMvc.perform(get("/api/v1/merchants/" + merchantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantCode").value("titan_watches"))
                .andExpect(jsonPath("$.businessName").value("Titan Watches Ltd"))
                .andExpect(jsonPath("$.active").value(true));

        // Verify GET /api/v1/merchants/{id}/dna (JSON-LD)
        mockMvc.perform(get("/api/v1/merchants/" + merchantId + "/dna"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.@type").value("MerchantIdentity"))
                .andExpect(jsonPath("$.merchantDid").value("did:cdna:merchant:titan_watches"))
                .andExpect(jsonPath("$.capabilities.agenticNegotiation").value(true))
                .andExpect(jsonPath("$.endpoints.manifest").value("/.well-known/commercedna.json"));

        // Verify GET /.well-known/commercedna.json?merchantCode=titan_watches
        mockMvc.perform(get("/.well-known/commercedna.json")
                        .param("merchantCode", "titan_watches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantCode").value("titan_watches"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request with Problem Detail when request validation fails")
    void shouldReturnBadRequestOnInvalidPayload() throws Exception {
        RegisterMerchantRequest invalidRequest = new RegisterMerchantRequest(
                "", // Blank
                "Test Merchant",
                "invalid-email-format",
                "",
                "",
                ""
        );

        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.invalidParams.contactEmail").isNotEmpty())
                .andExpect(jsonPath("$.invalidParams.merchantCode").isNotEmpty());
    }

    @Test
    @DisplayName("Should return 422 Problem Detail when merchantCode is already registered")
    void shouldReturn422OnDuplicateMerchantCode() throws Exception {
        RegisterMerchantRequest request = new RegisterMerchantRequest(
                "unique_brand",
                "Unique Brand Inc",
                "contact@uniquebrand.com",
                "rzp_test_1",
                "rzp_test_2",
                "rzp_test_3"
        );

        // First registration
        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate registration
        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Domain Rule Violation"))
                .andExpect(jsonPath("$.detail").value("Merchant with code 'unique_brand' is already registered."));
    }
}
