package io.commercedna.audit.scrubber;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PiiLogScrubberTest {

    private final PiiLogScrubber scrubber = new PiiLogScrubber();

    @Test
    @DisplayName("Should scrub credit card numbers and CVV codes")
    void shouldScrubFinancialDetails() {
        String raw = "User paid with card 4111111111111111 and cvv: 123";
        String scrubbed = scrubber.scrub(raw);

        assertFalse(scrubbed.contains("4111111111111111"));
        assertFalse(scrubbed.contains("123"));
        assertTrue(scrubbed.contains("****-****-****-****"));
        assertTrue(scrubbed.contains("[REDACTED_CVV]"));
    }

    @Test
    @DisplayName("Should scrub API keys and private keys in JSON")
    void shouldScrubApiKeys() {
        String raw = "{\"key_secret\": \"rzp_test_secret_abc12345678\", \"privKey\": \"dGVzdF9wcml2YXRlX2tleV8xMjM0NQ==\"}";
        String scrubbed = scrubber.scrub(raw);

        assertFalse(scrubbed.contains("rzp_test_secret_abc12345678"));
        assertFalse(scrubbed.contains("dGVzdF9wcml2YXRlX2tleV8xMjM0NQ=="));
        assertTrue(scrubbed.contains("[REDACTED_SECRET]"));
    }
}
