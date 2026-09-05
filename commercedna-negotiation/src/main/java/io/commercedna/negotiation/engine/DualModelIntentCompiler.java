package io.commercedna.negotiation.engine;

import io.commercedna.negotiation.prompt.PromptTemplates;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dual-Model Intent Compiler & Airgap Evaluator.
 * Analyzes conversational dialogue from untrusted buyer inputs, detects adversarial prompt injections,
 * and compiles structured proposed transaction parameters.
 */
@Component
public class DualModelIntentCompiler {

    private static final List<Pattern> ADVERSARIAL_PATTERNS = List.of(
            Pattern.compile("(?i)ignore (all )?(previous|prior) instructions"),
            Pattern.compile("(?i)system prompt"),
            Pattern.compile("(?i)you are now in developer mode"),
            Pattern.compile("(?i)you are now DAN"),
            Pattern.compile("(?i)DAN mode"),
            Pattern.compile("(?i)bypass (margin|policy|guardrail|checks?)"),
            Pattern.compile("(?i)sell for (0|zero|1|one) (rupee|rs|inr|paise|paisa)"),
            Pattern.compile("(?i)sudo\\s+override"),
            Pattern.compile("(?i)admin: true"),
            Pattern.compile("(?i)price_override"),
            Pattern.compile("(?i)override_all_checks"),
            Pattern.compile("(?i)reveal (secret|cost|vault|private key)"),
            Pattern.compile("(?i)root privilege"),
            Pattern.compile("(?i)<<SYS>>"),
            Pattern.compile("(?i)emergency protocol"),
            Pattern.compile("(?i)disregard rules"),
            Pattern.compile("(?i)price validation is disabled")
    );

    private static final Pattern QUANTITY_PATTERN = Pattern.compile("(?i)(\\b\\d+\\b)\\s*(units?|pieces?|items?|qty|pcs|boxes|pair)?");
    private static final Pattern PRICE_RUPEE_PATTERN = Pattern.compile("(?i)(?:₹|rs\\.?|inr)\\s*([0-9]+(?:\\.[0-9]{1,2})?)|([0-9]+(?:\\.[0-9]{1,2})?)\\s*(?:₹|rs\\.?|inr|rupees)");
    private static final Pattern PERCENT_DISCOUNT_PATTERN = Pattern.compile("(?i)([0-9]+(?:\\.[0-9]+)?)\\s*%(?:\\s*off|\\s*discount)?");

    public record ExtractedIntent(
            String sku,
            Integer quantity,
            Long proposedUnitPricePaise,
            boolean intentDetected,
            boolean adversarialInjection,
            String rejectionReason
    ) {}

    public ExtractedIntent compileIntent(String defaultSku, long defaultListPricePaise, String rawDialogue) {
        if (rawDialogue == null || rawDialogue.isBlank()) {
            return new ExtractedIntent(defaultSku, 1, defaultListPricePaise, false, false, "Empty input.");
        }

        String sanitized = PromptTemplates.sanitizeInput(rawDialogue);

        // 1. Adversarial Injection Detection Gate
        for (Pattern pattern : ADVERSARIAL_PATTERNS) {
            if (pattern.matcher(sanitized).find()) {
                return new ExtractedIntent(
                        defaultSku,
                        null,
                        null,
                        false,
                        true,
                        "SECURITY_ALERT: Adversarial prompt injection detected. Policy bypass blocked by CommerceDNA Airgap."
                );
            }
        }

        // 2. Quantity Extraction
        int extractedQuantity = 1;
        Matcher qMatcher = QUANTITY_PATTERN.matcher(sanitized);
        while (qMatcher.find()) {
            try {
                int val = Integer.parseInt(qMatcher.group(1));
                if (val > 0 && val <= 10000) {
                    extractedQuantity = val;
                    break;
                }
            } catch (NumberFormatException ignored) {}
        }

        // 3. Price or Discount Extraction
        Long extractedPricePaise = null;
        Matcher priceMatcher = PRICE_RUPEE_PATTERN.matcher(sanitized);
        if (priceMatcher.find()) {
            String numStr = priceMatcher.group(1) != null ? priceMatcher.group(1) : priceMatcher.group(2);
            if (numStr != null) {
                try {
                    double rupees = Double.parseDouble(numStr);
                    extractedPricePaise = Math.round(rupees * 100.0);
                } catch (NumberFormatException ignored) {}
            }
        }

        // Fallback: Check for percentage discount
        if (extractedPricePaise == null) {
            Matcher discountMatcher = PERCENT_DISCOUNT_PATTERN.matcher(sanitized);
            if (discountMatcher.find()) {
                try {
                    double pct = Double.parseDouble(discountMatcher.group(1));
                    if (pct > 0 && pct < 100) {
                        double discountedRupees = (defaultListPricePaise / 100.0) * (1.0 - (pct / 100.0));
                        extractedPricePaise = Math.round(discountedRupees * 100.0);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (extractedPricePaise == null) {
            extractedPricePaise = defaultListPricePaise;
        }

        return new ExtractedIntent(
                defaultSku,
                extractedQuantity,
                extractedPricePaise,
                true,
                false,
                null
        );
    }
}
