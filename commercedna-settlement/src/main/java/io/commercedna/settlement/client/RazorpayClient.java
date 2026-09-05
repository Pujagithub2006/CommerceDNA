package io.commercedna.settlement.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Typed Client for Razorpay Test Mode Settlement Rails.
 * Generates Razorpay Orders, Payment Links, and handles simulation sandboxes.
 */
@Component
public class RazorpayClient {

    private static final Logger log = LoggerFactory.getLogger(RazorpayClient.class);

    private final RazorpayProperties properties;

    public RazorpayClient(RazorpayProperties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    public record RazorpayOrderResult(
            String id,
            long amountPaise,
            String currency,
            String receipt,
            String status,
            Map<String, String> notes
    ) {}

    public record RazorpayPaymentLinkResult(
            String id,
            String shortUrl,
            long amountPaise,
            String currency,
            String description,
            String status
    ) {}

    public RazorpayOrderResult createOrder(long amountPaise, String currency, String receipt, Map<String, String> notes) {
        if (amountPaise <= 0) {
            throw new IllegalArgumentException("Razorpay Order amount must be positive integer paise.");
        }

        String orderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        log.info("Created Razorpay Test Mode Order: ID={}, Amount={} paise, Currency={}", orderId, amountPaise, currency);

        return new RazorpayOrderResult(
                orderId,
                amountPaise,
                currency != null ? currency : "INR",
                receipt,
                "created",
                notes != null ? notes : Map.of()
        );
    }

    public RazorpayPaymentLinkResult createPaymentLink(long amountPaise, String currency, String description, String customerName, String customerEmail, Map<String, String> notes) {
        if (amountPaise <= 0) {
            throw new IllegalArgumentException("Razorpay Payment Link amount must be positive integer paise.");
        }

        String linkId = "plink_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        String shortUrl = "https://rzp.io/i/" + linkId.substring(6);
        log.info("Generated Razorpay Hosted Payment Link: ID={}, URL={}, Amount={} paise", linkId, shortUrl, amountPaise);

        return new RazorpayPaymentLinkResult(
                linkId,
                shortUrl,
                amountPaise,
                currency != null ? currency : "INR",
                description,
                "created"
        );
    }

    public record RazorpayRefundResult(
            String id,
            String paymentId,
            long amountPaise,
            String currency,
            String status,
            String reason
    ) {}

    public RazorpayRefundResult createRefund(String paymentId, long amountPaise, String reason) {
        if (amountPaise <= 0) {
            throw new IllegalArgumentException("Razorpay Refund amount must be positive integer paise.");
        }

        String refundId = "rfnd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        log.info("Processed Razorpay Test Mode Refund: ID={}, PaymentID={}, Amount={} paise", refundId, paymentId, amountPaise);

        return new RazorpayRefundResult(
                refundId,
                paymentId,
                amountPaise,
                "INR",
                "processed",
                reason != null ? reason : "Customer initiated refund"
        );
    }
}
