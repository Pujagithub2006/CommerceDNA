package io.commercedna.settlement.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.commercedna.core.entity.OrderStatus;
import io.commercedna.core.exception.CryptoVerificationException;
import io.commercedna.core.exception.ResourceNotFoundException;
import io.commercedna.settlement.client.RazorpayProperties;
import io.commercedna.settlement.entity.OrderEntity;
import io.commercedna.settlement.entity.OutboxEventEntity;
import io.commercedna.settlement.repository.OrderJpaRepository;
import io.commercedna.settlement.repository.OutboxJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

/**
 * Verifies Razorpay Webhook HMAC-SHA256 signatures in constant time and transitions order states.
 */
@Service
@Transactional
public class WebhookProcessor {

    private static final Logger log = LoggerFactory.getLogger(WebhookProcessor.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final OrderJpaRepository orderRepository;
    private final OutboxJpaRepository outboxRepository;
    private final RazorpayProperties razorpayProperties;
    private final ObjectMapper objectMapper;

    public WebhookProcessor(
            OrderJpaRepository orderRepository,
            OutboxJpaRepository outboxRepository,
            RazorpayProperties razorpayProperties,
            ObjectMapper objectMapper
    ) {
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.outboxRepository = Objects.requireNonNull(outboxRepository);
        this.razorpayProperties = Objects.requireNonNull(razorpayProperties);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public record WebhookProcessResult(
            boolean verified,
            String eventType,
            String orderCode,
            OrderStatus newStatus,
            String message
    ) {}

    /**
     * Process a Razorpay webhook using the vault-encrypted webhook secret.
     * This is the canonical entry-point; the secret is never caller-supplied.
     */
    public WebhookProcessResult processWebhook(String rawPayload, String signatureHeader) {
        return processWebhook(rawPayload, signatureHeader, null);
    }

    /**
     * Internal overload retained for test compatibility.
     * {@code customSecret} MUST be {@code null} in all production call-sites;
     * the vault secret is always used when customSecret is null or blank.
     *
     * @deprecated Use {@link #processWebhook(String, String)} instead.
     */
    @Deprecated
    public WebhookProcessResult processWebhook(String rawPayload, String signatureHeader, String customSecret) {
        Objects.requireNonNull(rawPayload, "rawPayload must not be null");

        // Production code never provides a customSecret; only test stubs may do so.
        String secret = (customSecret != null && !customSecret.isBlank())
                ? customSecret
                : razorpayProperties.getWebhookSecret();

        // 1. Constant-time HMAC-SHA256 Signature Verification
        if (!verifyHmacSignature(rawPayload, signatureHeader, secret)) {
            log.warn("Razorpay Webhook HMAC signature verification failed.");
            throw new CryptoVerificationException("Invalid Razorpay webhook signature. Request rejected.");
        }

        // 2. Parse Webhook Event JSON
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String event = root.path("event").asText("");
            log.info("Processing verified Razorpay Webhook event: {}", event);

            JsonNode payload = root.path("payload");
            String rzpOrderId = null;
            String rzpPaymentId = null;

            if (payload.has("payment") && payload.path("payment").has("entity")) {
                JsonNode paymentEntity = payload.path("payment").path("entity");
                rzpOrderId = paymentEntity.path("order_id").asText(null);
                rzpPaymentId = paymentEntity.path("id").asText(null);
            } else if (payload.has("order") && payload.path("order").has("entity")) {
                JsonNode orderEntity = payload.path("order").path("entity");
                rzpOrderId = orderEntity.path("id").asText(null);
            } else if (payload.has("payment_link") && payload.path("payment_link").has("entity")) {
                JsonNode plinkEntity = payload.path("payment_link").path("entity");
                String plinkId = plinkEntity.path("id").asText(null);
                if (plinkId != null) {
                    OrderEntity order = orderRepository.findByRazorpayPaymentLinkId(plinkId).orElse(null);
                    if (order != null) {
                        rzpOrderId = order.getRazorpayOrderId();
                    }
                }
            }

            if (rzpOrderId == null) {
                return new WebhookProcessResult(true, event, null, null, "Webhook acknowledged (non-order event)");
            }

            final String targetRzpOrderId = rzpOrderId;
            OrderEntity order = orderRepository.findByRazorpayOrderId(targetRzpOrderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Order with Razorpay ID", targetRzpOrderId));


            // 3. State Transition to PAID
            if (event.contains("paid") || event.contains("captured")) {
                order.setStatus(OrderStatus.PAID);
                if (rzpPaymentId != null) {
                    order.setRazorpayPaymentId(rzpPaymentId);
                }
                order.setUpdatedAt(Instant.now());
                orderRepository.save(order);

                // Enqueue Outbox event
                OutboxEventEntity outboxEvent = new OutboxEventEntity(
                        UUID.randomUUID(),
                        "PAYMENT_COMPLETED",
                        "ORDER",
                        order.getId().toString(),
                        objectMapper.writeValueAsString(order),
                        "PENDING",
                        0,
                        Instant.now(),
                        null
                );
                outboxRepository.save(outboxEvent);

                return new WebhookProcessResult(true, event, order.getOrderCode(), OrderStatus.PAID, "Order transitioned to PAID");
            }

            return new WebhookProcessResult(true, event, order.getOrderCode(), order.getStatus(), "Webhook processed successfully");

        } catch (Exception ex) {
            log.error("Failed to process webhook payload", ex);
            throw new RuntimeException("Webhook processing error: " + ex.getMessage(), ex);
        }
    }

    public boolean verifyHmacSignature(String payload, String signatureHeader, String secret) {
        if (signatureHeader == null || signatureHeader.isBlank() || secret == null || secret.isBlank()) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(rawHmac);

            // Constant-time comparison to prevent timing attacks
            return MessageDigest.isEqual(
                    computedHex.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.trim().getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            log.error("HMAC calculation error", ex);
            return false;
        }
    }
}
