package io.commercedna.api.security;

import org.owasp.encoder.Encode;
import org.springframework.stereotype.Component;

/**
 * Input Sanitization Utility for XSS and SQL Injection Prevention.
 * Uses OWASP Encoder for safe HTML/JavaScript output encoding.
 */
@Component
public class InputSanitizer {

    /**
     * Sanitizes user input to prevent XSS attacks.
     */
    public String sanitizeForHtml(String input) {
        if (input == null) {
            return null;
        }
        return Encode.forHtml(input);
    }

    /**
     * Sanitizes input for JavaScript context.
     */
    public String sanitizeForJavaScript(String input) {
        if (input == null) {
            return null;
        }
        return Encode.forJavaScript(input);
    }

    /**
     * Sanitizes input for CSS context.
     */
    public String sanitizeForCss(String input) {
        if (input == null) {
            return null;
        }
        return Encode.forCssString(input);
    }

    /**
     * Sanitizes input for URL parameters.
     */
    public String sanitizeForUrl(String input) {
        if (input == null) {
            return null;
        }
        return Encode.forUriComponent(input);
    }

    /**
     * Validates and sanitizes merchant codes to prevent injection.
     */
    public String sanitizeMerchantCode(String merchantCode) {
        if (merchantCode == null) {
            return null;
        }
        // Allow only alphanumeric, hyphens, and underscores
        return merchantCode.replaceAll("[^a-zA-Z0-9-_]", "").toLowerCase();
    }

    /**
     * Validates SKU format.
     */
    public boolean isValidSku(String sku) {
        if (sku == null || sku.isBlank()) {
            return false;
        }
        // SKU should be alphanumeric with hyphens, 3-50 characters
        return sku.matches("^[a-zA-Z0-9-]{3,50}$");
    }
}