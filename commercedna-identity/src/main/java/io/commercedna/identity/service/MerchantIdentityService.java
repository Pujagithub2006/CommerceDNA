package io.commercedna.identity.service;

import io.commercedna.core.entity.Merchant;
import io.commercedna.core.exception.DomainException;
import io.commercedna.core.exception.ResourceNotFoundException;
import io.commercedna.core.port.CryptoPort;
import io.commercedna.core.port.MerchantRepositoryPort;
import io.commercedna.core.port.SecretVaultPort;
import io.commercedna.identity.repository.MerchantEntity;
import io.commercedna.identity.repository.MerchantJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Core Merchant Identity Service.
 * Manages verifiable merchant onboarding, cryptographic DNA key generation,
 * encrypted secret persistence, and JSON-LD manifest rendering.
 */
@Service
@Transactional
public class MerchantIdentityService implements MerchantRepositoryPort {

    private final MerchantJpaRepository merchantRepository;
    private final CryptoPort cryptoPort;
    private final SecretVaultPort secretVault;

    public MerchantIdentityService(
            MerchantJpaRepository merchantRepository,
            CryptoPort cryptoPort,
            SecretVaultPort secretVault
    ) {
        this.merchantRepository = Objects.requireNonNull(merchantRepository);
        this.cryptoPort = Objects.requireNonNull(cryptoPort);
        this.secretVault = Objects.requireNonNull(secretVault);
    }

    public record RegistrationResult(
            Merchant merchant,
            String privateKeyEd25519Base64,
            String publicKeyEd25519Base64,
            String merchantDid
    ) {}

    public record RegisterMerchantCommand(
            String merchantCode,
            String businessName,
            String contactEmail,
            String rawRazorpayKeyId,
            String rawRazorpayKeySecret,
            String rawWebhookSecret
    ) {}

    public RegistrationResult registerMerchant(RegisterMerchantCommand cmd) {
        Objects.requireNonNull(cmd, "RegisterMerchantCommand must not be null");
        String normalizedCode = cmd.merchantCode().trim().toLowerCase();

        if (merchantRepository.existsByMerchantCode(normalizedCode)) {
            throw new DomainException("Merchant with code '" + normalizedCode + "' is already registered.");
        }

        // 1. Generate Sovereign Ed25519 Keypair
        CryptoPort.KeyPairResult keyPair = cryptoPort.generateEd25519KeyPair();

        // 2. Encrypt Razorpay Secrets using AES-256-GCM Vault
        String encKeyId = secretVault.encrypt(cmd.rawRazorpayKeyId().trim());
        String encKeySecret = secretVault.encrypt(cmd.rawRazorpayKeySecret().trim());
        String encWebhookSecret = secretVault.encrypt(cmd.rawWebhookSecret().trim());

        // 3. Persist Entity
        UUID merchantId = UUID.randomUUID();
        Instant now = Instant.now();

        MerchantEntity entity = new MerchantEntity(
                merchantId,
                normalizedCode,
                cmd.businessName().trim(),
                cmd.contactEmail().trim(),
                keyPair.publicKeyBase64(),
                encKeyId,
                encKeySecret,
                encWebhookSecret,
                true,
                now,
                now
        );

        merchantRepository.save(entity);

        Merchant domainMerchant = mapToDomain(entity);
        return new RegistrationResult(
                domainMerchant,
                keyPair.privateKeyBase64(),
                keyPair.publicKeyBase64(),
                domainMerchant.getDid().getValue()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> renderJsonLdManifest(UUID merchantId) {
        MerchantEntity entity = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("@context", "https://schema.org");
        manifest.put("@type", "MerchantIdentity");
        manifest.put("merchantId", entity.getId().toString());
        manifest.put("merchantCode", entity.getMerchantCode());
        manifest.put("businessName", entity.getBusinessName());
        manifest.put("merchantDid", "did:cdna:merchant:" + entity.getMerchantCode());
        manifest.put("publicKeyEd25519", entity.getPublicKeyEd25519());
        manifest.put("protocolVersion", "1.0.0");
        manifest.put("settlementCurrency", "INR");

        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("semanticDiscovery", true);
        capabilities.put("agenticNegotiation", true);
        capabilities.put("razorpayTestModeSettlement", true);
        capabilities.put("cryptographicAuditLedger", true);
        manifest.put("capabilities", capabilities);

        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("manifest", "/.well-known/commercedna.json");
        endpoints.put("search", "/api/v1/catalog/search");
        endpoints.put("negotiate", "/api/v1/negotiate/propose");
        endpoints.put("settlement", "/api/v1/settlement/orders");
        endpoints.put("webhooks", "/api/v1/webhooks/razorpay");
        manifest.put("endpoints", endpoints);

        return manifest;
    }

    public record DecryptedRazorpayCredentials(String keyId, String keySecret, String webhookSecret) {}

    @Transactional(readOnly = true)
    public DecryptedRazorpayCredentials getDecryptedCredentials(UUID merchantId) {
        MerchantEntity entity = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant", merchantId.toString()));

        return new DecryptedRazorpayCredentials(
                secretVault.decrypt(entity.getEncryptedRazorpayKeyId()),
                secretVault.decrypt(entity.getEncryptedRazorpayKeySecret()),
                secretVault.decrypt(entity.getWebhookSecret())
        );
    }

    @Override
    public Merchant save(Merchant merchant) {
        MerchantEntity entity = merchantRepository.findById(merchant.getId())
                .orElseGet(MerchantEntity::new);

        entity.setId(merchant.getId());
        entity.setMerchantCode(merchant.getMerchantCode());
        entity.setBusinessName(merchant.getBusinessName());
        entity.setContactEmail(merchant.getContactEmail());
        entity.setPublicKeyEd25519(merchant.getPublicKeyEd25519());
        entity.setActive(merchant.isActive());
        entity.setUpdatedAt(Instant.now());

        merchantRepository.save(entity);
        return merchant;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Merchant> findById(UUID id) {
        return merchantRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Merchant> findByMerchantCode(String merchantCode) {
        return merchantRepository.findByMerchantCode(merchantCode.trim().toLowerCase()).map(this::mapToDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByMerchantCode(String merchantCode) {
        return merchantRepository.existsByMerchantCode(merchantCode.trim().toLowerCase());
    }

    private Merchant mapToDomain(MerchantEntity entity) {
        return new Merchant(
                entity.getId(),
                entity.getMerchantCode(),
                entity.getBusinessName(),
                entity.getContactEmail(),
                entity.getPublicKeyEd25519(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
