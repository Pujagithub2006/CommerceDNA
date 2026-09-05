package io.commercedna.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterMerchantRequest(
        @NotBlank(message = "merchantCode is required")
        @Pattern(regexp = "^[a-zA-Z0-9_-]{3,32}$", message = "merchantCode must be 3-32 alphanumeric characters, dashes, or underscores")
        String merchantCode,

        @NotBlank(message = "businessName is required")
        String businessName,

        @NotBlank(message = "contactEmail is required")
        @Email(message = "contactEmail must be a valid email address")
        String contactEmail,

        @NotBlank(message = "rawRazorpayKeyId is required")
        String rawRazorpayKeyId,

        @NotBlank(message = "rawRazorpayKeySecret is required")
        String rawRazorpayKeySecret,

        @NotBlank(message = "rawWebhookSecret is required")
        String rawWebhookSecret
) {}
