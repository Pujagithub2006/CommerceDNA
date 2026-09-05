package io.commercedna.settlement.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RazorpayClientTest {

    private RazorpayClient client;

    @BeforeEach
    void setUp() {
        RazorpayProperties props = new RazorpayProperties();
        props.setKeyId("rzp_test_mock");
        props.setKeySecret("secret_mock");
        client = new RazorpayClient(props);
    }

    @Test
    @DisplayName("Should create Razorpay Test Mode Order with integer paise precision")
    void shouldCreateOrder() {
        RazorpayClient.RazorpayOrderResult result = client.createOrder(
                4400000L,
                "INR",
                "receipt_001",
                Map.of("merchantCode", "apex-tech")
        );

        assertNotNull(result.id());
        assertTrue(result.id().startsWith("order_"));
        assertEquals(4400000L, result.amountPaise());
        assertEquals("INR", result.currency());
        assertEquals("created", result.status());
    }

    @Test
    @DisplayName("Should generate Razorpay Hosted Payment Link")
    void shouldCreatePaymentLink() {
        RazorpayClient.RazorpayPaymentLinkResult result = client.createPaymentLink(
                4400000L,
                "INR",
                "10x Headphones",
                "Buyer Agent",
                "buyer@agent.net",
                Map.of()
        );

        assertNotNull(result.id());
        assertTrue(result.id().startsWith("plink_"));
        assertTrue(result.shortUrl().startsWith("https://rzp.io/i/"));
        assertEquals(4400000L, result.amountPaise());
    }
}
