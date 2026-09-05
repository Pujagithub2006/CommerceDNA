package io.commercedna.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Protocol Liveness and Capabilities Health Check Endpoint.
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "System Health", description = "Liveness, readiness, and protocol capability checks")
public class HealthController {

    @GetMapping
    @Operation(summary = "Protocol Health and Capability Discovery", description = "Returns system liveness and supported agentic commerce protocols")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "commercedna-api");
        response.put("version", "1.0.0");
        response.put("timestamp", Instant.now().toString());
        response.put("protocolsSupported", List.of(
                "cdna/1.0.0",
                "razorpay-test-mode/v1",
                "w3c-trace-context/v1",
                "ed25519-did/v1"
        ));
        response.put("capabilities", Map.of(
                "semanticDiscovery", true,
                "agenticNegotiation", true,
                "deterministicNegotiation", true,
                "razorpayTestModeSettlement", true,
                "cryptographicAuditLedger", true
        ));

        return ResponseEntity.ok(response);
    }
}
