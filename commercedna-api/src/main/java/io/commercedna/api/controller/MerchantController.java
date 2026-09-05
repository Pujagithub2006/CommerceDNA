package io.commercedna.api.controller;

import io.commercedna.api.dto.MerchantProfileResponse;
import io.commercedna.api.dto.RegisterMerchantRequest;
import io.commercedna.api.dto.RegisterMerchantResponse;
import io.commercedna.core.entity.Merchant;
import io.commercedna.core.exception.ResourceNotFoundException;
import io.commercedna.identity.service.MerchantIdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Controller handling Merchant Onboarding, DNA Key Generation, and Machine-Readable Manifests.
 */
@RestController
@Tag(name = "Merchant DNA & Identity", description = "Merchant onboarding, cryptographic DNA, and JSON-LD discovery")
public class MerchantController {

    private final MerchantIdentityService merchantIdentityService;

    public MerchantController(MerchantIdentityService merchantIdentityService) {
        this.merchantIdentityService = Objects.requireNonNull(merchantIdentityService);
    }

    @PostMapping("/api/v1/merchants")
    @Operation(summary = "Register Merchant", description = "Onboards a new merchant, generates Ed25519 identity keypair, and encrypts Razorpay API credentials into AES-256-GCM vault")
    public ResponseEntity<RegisterMerchantResponse> registerMerchant(@Valid @RequestBody RegisterMerchantRequest request) {
        MerchantIdentityService.RegisterMerchantCommand cmd = new MerchantIdentityService.RegisterMerchantCommand(
                request.merchantCode(),
                request.businessName(),
                request.contactEmail(),
                request.rawRazorpayKeyId(),
                request.rawRazorpayKeySecret(),
                request.rawWebhookSecret()
        );

        MerchantIdentityService.RegistrationResult result = merchantIdentityService.registerMerchant(cmd);
        Merchant merchant = result.merchant();

        RegisterMerchantResponse response = new RegisterMerchantResponse(
                merchant.getId(),
                merchant.getMerchantCode(),
                merchant.getBusinessName(),
                merchant.getContactEmail(),
                result.merchantDid(),
                result.publicKeyEd25519Base64(),
                result.privateKeyEd25519Base64(),
                merchant.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/merchants/{id}")
    @Operation(summary = "Get Merchant Profile", description = "Fetches public profile and cryptographic DID of a merchant")
    public ResponseEntity<MerchantProfileResponse> getMerchantProfile(@PathVariable UUID id) {
        Merchant merchant = merchantIdentityService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", id.toString()));

        MerchantProfileResponse response = new MerchantProfileResponse(
                merchant.getId(),
                merchant.getMerchantCode(),
                merchant.getBusinessName(),
                merchant.getContactEmail(),
                merchant.getDid().getValue(),
                merchant.getPublicKeyEd25519(),
                merchant.isActive(),
                merchant.getCreatedAt(),
                merchant.getUpdatedAt()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/v1/merchants/{id}/dna")
    @Operation(summary = "Get Merchant JSON-LD DNA", description = "Renders machine-readable JSON-LD merchant manifest containing cryptographic public key and agent capabilities")
    public ResponseEntity<Map<String, Object>> getMerchantDna(@PathVariable UUID id) {
        Map<String, Object> manifest = merchantIdentityService.renderJsonLdManifest(id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(manifest);
    }

    @GetMapping(value = "/.well-known/commercedna.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Agentic Commerce Well-Known Discovery", description = "Standard protocol discovery manifest allowing autonomous Buyer Agents to identify merchant capabilities")
    public ResponseEntity<Map<String, Object>> getWellKnownManifest(@RequestParam(required = false) String merchantCode) {
        Merchant merchant;
        if (merchantCode != null && !merchantCode.isBlank()) {
            merchant = merchantIdentityService.findByMerchantCode(merchantCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Merchant with code", merchantCode));
        } else {
            merchant = merchantIdentityService.findByMerchantCode("default")
                    .or(() -> merchantIdentityService.findByMerchantCode("demo_store"))
                    .orElseThrow(() -> new ResourceNotFoundException("Default Merchant", "No merchant found. Register a merchant first or provide ?merchantCode=..."));
        }

        Map<String, Object> manifest = merchantIdentityService.renderJsonLdManifest(merchant.getId());
        return ResponseEntity.ok(manifest);
    }
}
