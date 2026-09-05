package io.commercedna.api.controller;

import io.commercedna.audit.engine.AuditLedgerEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLedgerEngine auditLedgerEngine;

    @Test
    @DisplayName("GET /api/v1/audit/ledger and POST /api/v1/audit/verify - should record and verify cryptographic ledger")
    void shouldRecordAndVerifyLedger() throws Exception {
        // Record test blocks
        auditLedgerEngine.recordEvent("TEST_EVENT_1", "MERCHANT", "m_test_1", Map.of("action", "test_action_1"));
        auditLedgerEngine.recordEvent("TEST_EVENT_2", "ORDER", "ord_test_2", Map.of("action", "test_action_2"));

        // Verify GET /api/v1/audit/ledger
        mockMvc.perform(get("/api/v1/audit/ledger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Verify POST /api/v1/audit/verify
        mockMvc.perform(post("/api/v1/audit/verify")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intact").value(true))
                .andExpect(jsonPath("$.totalBlocksVerified").isNumber());

        // Verify GET /api/v1/analytics/overview
        mockMvc.perform(get("/api/v1/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.marginProtectionStatus").value("ACTIVE"));
    }
}
