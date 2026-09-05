package io.commercedna.audit.scrubber;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * PII and Secret Scrubber.
 * Ensures zero credential or financial card leaks in structured audit payloads and logs.
 */
@Component
public class PiiLogScrubber {

    private static final Pattern CARD_PAN_PATTERN = Pattern.compile("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|6(?:011|5[0-9]{2})[0-9]{12})\\b");
    private static final Pattern CVV_PATTERN = Pattern.compile("(?i)([\"']?(?:cvv|cvc|cvn|cid)[\"']?\\s*[:=]\\s*[\"']?)[0-9]{3,4}([\"']?)");
    private static final Pattern SECRET_PATTERN = Pattern.compile("(?i)([\"']?(?:key_secret|webhook_secret|secret|privateKey|private_key|privKey)[\"']?\\s*[:=]\\s*[\"']?)[^\"'\\s,}\\]]{6,}([\"']?)");

    public String scrub(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String scrubbed = input;
        scrubbed = CARD_PAN_PATTERN.matcher(scrubbed).replaceAll("****-****-****-****");
        scrubbed = CVV_PATTERN.matcher(scrubbed).replaceAll("$1[REDACTED_CVV]$2");
        scrubbed = SECRET_PATTERN.matcher(scrubbed).replaceAll("$1[REDACTED_SECRET]$2");

        return scrubbed;
    }
}
