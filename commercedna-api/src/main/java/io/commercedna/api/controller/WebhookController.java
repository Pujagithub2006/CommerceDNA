package io.commercedna.api.controller;

import io.commercedna.settlement.webhook.WebhookProcessor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

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
        WebhookProcessor.WebhookProcessResult result = webhookProcessor.processWebhook(rawPayload, signatureHeader, null);
        return ResponseEntity.ok(result);
    }
}
