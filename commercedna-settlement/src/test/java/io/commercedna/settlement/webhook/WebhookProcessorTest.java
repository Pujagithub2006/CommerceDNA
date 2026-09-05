package io.commercedna.settlement.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.commercedna.core.entity.OrderStatus;
import io.commercedna.core.exception.CryptoVerificationException;
import io.commercedna.settlement.client.RazorpayProperties;
import io.commercedna.settlement.entity.OrderEntity;
import io.commercedna.settlement.entity.OutboxEventEntity;
import io.commercedna.settlement.repository.OrderJpaRepository;
import io.commercedna.settlement.repository.OutboxJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookProcessorTest {

    @Mock
    private OrderJpaRepository orderRepository;

    @Mock
    private OutboxJpaRepository outboxRepository;

    private RazorpayProperties properties;
    private ObjectMapper objectMapper;
    private WebhookProcessor webhookProcessor;

    private static final String WEBHOOK_SECRET = "test_webhook_secret_key_12345";

    @BeforeEach
    void setUp() {
        properties = new RazorpayProperties();
        properties.setWebhookSecret(WEBHOOK_SECRET);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        webhookProcessor = new WebhookProcessor(orderRepository, outboxRepository, properties, objectMapper);

    }

    private String generateSignature(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("Should verify valid Razorpay HMAC signature and transition order to PAID")
    void shouldProcessValidWebhook() throws Exception {
        String payload = """
                {
                  "entity": "event",
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_test_999",
                        "order_id": "order_test_123",
                        "amount": 4400000,
                        "currency": "INR",
                        "status": "captured"
                      }
                    }
                  }
                }
                """;

        String signature = generateSignature(payload, WEBHOOK_SECRET);

        OrderEntity order = new OrderEntity(
                UUID.randomUUID(),
                "ord_123",
                UUID.randomUUID(),
                "did:cdna:buyer",
                "prop_123",
                "idemp_123",
                "AURORA-ANC-001",
                10,
                440000L,
                4400000L,
                "INR",
                OrderStatus.CREATED,
                "order_test_123",
                null,
                "plink_123",
                "https://rzp.io/i/123",
                Instant.now(),
                Instant.now()
        );

        when(orderRepository.findByRazorpayOrderId("order_test_123")).thenReturn(Optional.of(order));

        WebhookProcessor.WebhookProcessResult result = webhookProcessor.processWebhook(payload, signature, WEBHOOK_SECRET);

        assertTrue(result.verified());
        assertEquals(OrderStatus.PAID, result.newStatus());
        assertEquals("ord_123", result.orderCode());
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals("pay_test_999", order.getRazorpayPaymentId());
        verify(orderRepository, times(1)).save(order);
        verify(outboxRepository, times(1)).save(any(OutboxEventEntity.class));
    }

    @Test
    @DisplayName("Should reject forged webhook payload with invalid signature")
    void shouldRejectForgedWebhook() {
        String payload = "{\"event\":\"order.paid\"}";
        String forgedSignature = "invalid_signature_hex_123";

        assertThrows(CryptoVerificationException.class, () ->
                webhookProcessor.processWebhook(payload, forgedSignature, WEBHOOK_SECRET)
        );
        verify(orderRepository, never()).save(any());
    }
}
