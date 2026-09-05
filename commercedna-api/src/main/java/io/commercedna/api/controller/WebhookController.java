package io.commercedna.api.controller;

import io.commercedna.settlement.webhook.WebhookProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * Razorpay Webhook Ingestion Controller.
 *
 * <p>The {@code X-Razorpay-Signature} header is marked required=false at the
 * Spring layer so that a missing header does NOT produce a 400 (which leaks
 * endpoint awareness). Instead the WebhookProcessor performs constant-time
 * HMAC comparison and throws {@link io.commercedna.core.exception.CryptoVerificationException},
 * which the GlobalExceptionHandler maps to 401. This pattern is intentional:
 * we always verify before revealing whether the endpoint exists.</p>
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Cryptographically verified Razorpay webhook ingestion and event finality")
public class WebhookController {

    private final WebhookProcessor webhookProcessor;

    public WebhookController(WebhookProcessor webhookProcessor) {
        this.webhookProcessor = Objects.requireNonNull(webhookProcessor);
    }

    @PostMapping("/razorpay")
    @Operation(summary = "Ingest Razorpay webhook with constant-time HMAC-SHA256 signature verification")
    public ResponseEntity<WebhookProcessor.WebhookProcessResult> handleRazorpayWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signatureHeader
    ) {
        // Never pass a caller-supplied secret – always use the vault-encrypted webhook secret.
        WebhookProcessor.WebhookProcessResult result = webhookProcessor.processWebhook(rawPayload, signatureHeader);
        return ResponseEntity.ok(result);
    }
}
