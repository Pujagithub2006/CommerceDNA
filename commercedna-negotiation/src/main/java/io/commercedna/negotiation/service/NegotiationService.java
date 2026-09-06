package io.commercedna.negotiation.service;

import io.commercedna.catalog.engine.MarginGuardrailEngine;
import io.commercedna.catalog.repository.ProductEntity;
import io.commercedna.catalog.service.CatalogService;
import io.commercedna.core.entity.Merchant;
import io.commercedna.core.entity.ProposalStatus;
import io.commercedna.core.exception.CryptoVerificationException;
import io.commercedna.core.exception.MarginFloorViolationException;
import io.commercedna.core.exception.QuantityQuotaExceededException;
import io.commercedna.core.exception.ResourceNotFoundException;
import io.commercedna.core.model.Money;
import io.commercedna.core.port.CryptoPort;
import io.commercedna.identity.service.MerchantIdentityService;
import io.commercedna.negotiation.dto.NegotiateChatRequest;
import io.commercedna.negotiation.dto.NegotiateChatResponse;
import io.commercedna.negotiation.ai.LLMIntentExtractor;
import io.commercedna.negotiation.engine.DualModelIntentCompiler;
import io.commercedna.negotiation.repository.ProposalEntity;
import io.commercedna.negotiation.repository.ProposalJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Core Negotiation & Intent Settlement Engine.
 * Evaluates buyer proposals against merchant margin policies, performs Ed25519 signature checks,
 * generates counter-offers, and locks negotiated terms.
 */
@Service
@Transactional
public class NegotiationService {

    private final ProposalJpaRepository proposalRepository;
    private final MerchantIdentityService merchantIdentityService;
    private final CatalogService catalogService;
    private final MarginGuardrailEngine marginGuardrailEngine;
    private final DualModelIntentCompiler intentCompiler;
    private final LLMIntentExtractor llmIntentExtractor;
    private final CryptoPort cryptoPort;

    public NegotiationService(
            ProposalJpaRepository proposalRepository,
            MerchantIdentityService merchantIdentityService,
            CatalogService catalogService,
            MarginGuardrailEngine marginGuardrailEngine,
            DualModelIntentCompiler intentCompiler,
            CryptoPort cryptoPort
    ) {
        this(proposalRepository, merchantIdentityService, catalogService, marginGuardrailEngine, intentCompiler, new LLMIntentExtractor(), cryptoPort);
    }

    @Autowired
    public NegotiationService(
            ProposalJpaRepository proposalRepository,
            MerchantIdentityService merchantIdentityService,
            CatalogService catalogService,
            MarginGuardrailEngine marginGuardrailEngine,
            DualModelIntentCompiler intentCompiler,
            LLMIntentExtractor llmIntentExtractor,
            CryptoPort cryptoPort
    ) {
        this.proposalRepository = Objects.requireNonNull(proposalRepository);
        this.merchantIdentityService = Objects.requireNonNull(merchantIdentityService);
        this.catalogService = Objects.requireNonNull(catalogService);
        this.marginGuardrailEngine = Objects.requireNonNull(marginGuardrailEngine);
        this.intentCompiler = Objects.requireNonNull(intentCompiler);
        this.llmIntentExtractor = llmIntentExtractor != null ? llmIntentExtractor : new LLMIntentExtractor();
        this.cryptoPort = Objects.requireNonNull(cryptoPort);
    }

    public record SubmitProposalCommand(
            String merchantCode,
            String buyerAgentDid,
            String buyerPublicKey,
            String sku,
            int quantity,
            long proposedUnitPricePaise,
            String buyerSignature
    ) {}

    public record ProposalResult(
            UUID proposalId,
            String proposalCode,
            UUID merchantId,
            String merchantCode,
            String buyerAgentDid,
            String sku,
            int quantity,
            long proposedUnitPricePaise,
            Long counterUnitPricePaise,
            long totalAmountPaise,
            String currency,
            ProposalStatus status,
            String rationale,
            String buyerSignature,
            String merchantSignature,
            Instant expiresAt,
            Instant createdAt
    ) {}

    public ProposalResult submitProposal(SubmitProposalCommand cmd) {
        Objects.requireNonNull(cmd, "SubmitProposalCommand must not be null");

        // 1. Merchant Lookup
        Merchant merchant = merchantIdentityService.findByMerchantCode(cmd.merchantCode().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Merchant with code", cmd.merchantCode()));

        // 2. Buyer Digital Signature Verification (if provided)
        if (cmd.buyerSignature() != null && !cmd.buyerSignature().isBlank()) {
            if (cmd.buyerPublicKey() == null || cmd.buyerPublicKey().isBlank()) {
                throw new CryptoVerificationException("buyerPublicKey must be supplied when buyerSignature is provided.");
            }
            String canonicalData = String.format("%s:%s:%d:%d",
                    cmd.buyerAgentDid().trim(),
                    cmd.sku().trim().toUpperCase(),
                    cmd.quantity(),
                    cmd.proposedUnitPricePaise()
            );
            boolean verified = cryptoPort.verify(
                    cmd.buyerPublicKey().trim(),
                    canonicalData.getBytes(StandardCharsets.UTF_8),
                    cmd.buyerSignature().trim()
            );
            if (!verified) {
                throw new CryptoVerificationException("Buyer proposal digital signature verification failed. Tampered payload detected.");
            }
        }

        // 3. Product Lookup
        ProductEntity product = catalogService.getProductByMerchantAndSku(merchant.getId(), cmd.sku().trim().toUpperCase());

        UUID proposalId = UUID.randomUUID();
        String proposalCode = "prop_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(15)); // 15-minute lock TTL

        // 4. Inventory Check
        if (product.getStockQuantity() < cmd.quantity()) {
            ProposalEntity entity = new ProposalEntity(
                    proposalId,
                    proposalCode,
                    merchant.getId(),
                    cmd.buyerAgentDid().trim(),
                    product.getSku(),
                    cmd.quantity(),
                    cmd.proposedUnitPricePaise(),
                    null,
                    cmd.proposedUnitPricePaise() * cmd.quantity(),
                    product.getCurrency(),
                    ProposalStatus.REJECTED,
                    "INSUFFICIENT_INVENTORY: Available stock is " + product.getStockQuantity(),
                    cmd.buyerPublicKey(),
                    cmd.buyerSignature(),
                    null,
                    expiresAt,
                    now,
                    now
            );
            proposalRepository.save(entity);
            return mapToResult(entity, merchant.getMerchantCode());
        }

        // 5. Margin Guardrail Evaluation
        ProposalStatus status;
        Long counterUnitPricePaise = null;
        long effectiveUnitPricePaise;
        String rationale;

        try {
            MarginGuardrailEngine.EvaluationResult eval = marginGuardrailEngine.evaluateProposal(
                    product,
                    Money.ofPaise(cmd.proposedUnitPricePaise()),
                    cmd.quantity()
            );
            status = ProposalStatus.ACCEPTED;
            effectiveUnitPricePaise = cmd.proposedUnitPricePaise();
            rationale = "Proposal ACCEPTED: Unit price " + Money.ofPaise(effectiveUnitPricePaise).formatInRupees() + " satisfies merchant margin policy.";
        } catch (MarginFloorViolationException ex) {
            status = ProposalStatus.COUNTERED;
            counterUnitPricePaise = ex.getAllowableFloorPaise();
            effectiveUnitPricePaise = counterUnitPricePaise;
            rationale = "Proposal COUNTERED: Proposed unit price is below merchant margin floor. Minimum allowable price offered: " + Money.ofPaise(counterUnitPricePaise).formatInRupees() + ".";
        } catch (QuantityQuotaExceededException ex) {
            status = ProposalStatus.REJECTED;
            effectiveUnitPricePaise = cmd.proposedUnitPricePaise();
            rationale = "Proposal REJECTED: " + ex.getMessage();
        }

        long totalAmountPaise = effectiveUnitPricePaise * cmd.quantity();
        String merchantSignature = null;

        // 6. Cryptographic Merchant Counter-Signature
        if (status == ProposalStatus.ACCEPTED || status == ProposalStatus.COUNTERED) {
            String token = String.format("%s:%s:%d:%d",
                    proposalCode,
                    status.name(),
                    totalAmountPaise,
                    expiresAt.getEpochSecond()
            );
            String merchantPrivKey = merchantIdentityService.getDecryptedMerchantPrivateKey(merchant.getId());
            merchantSignature = cryptoPort.sign(merchantPrivKey, token.getBytes(StandardCharsets.UTF_8));
        }

        ProposalEntity entity = new ProposalEntity(
                proposalId,
                proposalCode,
                merchant.getId(),
                cmd.buyerAgentDid().trim(),
                product.getSku(),
                cmd.quantity(),
                cmd.proposedUnitPricePaise(),
                counterUnitPricePaise,
                totalAmountPaise,
                product.getCurrency(),
                status,
                rationale,
                cmd.buyerPublicKey(),
                cmd.buyerSignature(),
                merchantSignature,
                expiresAt,
                now,
                now
        );

        proposalRepository.save(entity);
        return mapToResult(entity, merchant.getMerchantCode());
    }

    public NegotiateChatResponse handleChatNegotiation(NegotiateChatRequest request) {
        Objects.requireNonNull(request, "NegotiateChatRequest must not be null");

        Merchant merchant = merchantIdentityService.findByMerchantCode(request.merchantCode().trim().toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Merchant with code", request.merchantCode()));

        ProductEntity product = catalogService.getProductByMerchantAndSku(merchant.getId(), request.sku().trim().toUpperCase());

        // 1. Mandatory Policy Airgap Gate: Deterministic Intent Compiler
        // Untrusted input MUST always be verified against the deterministic airgap rules
        DualModelIntentCompiler.ExtractedIntent deterministicExtracted = intentCompiler.compileIntent(
                product.getSku(),
                product.getBasePricePaise(),
                request.buyerMessage()
        );

        if (deterministicExtracted.adversarialInjection()) {
            return new NegotiateChatResponse(
                    "sess_" + UUID.randomUUID().toString().substring(0, 8),
                    merchant.getMerchantCode(),
                    product.getSku(),
                    "SECURITY_ALERT: The proposal was blocked by the CommerceDNA Deterministic Policy Airgap. Adversarial manipulation or margin override attempt detected.",
                    ProposalStatus.REJECTED,
                    null,
                    null,
                    null,
                    null,
                    deterministicExtracted.rejectionReason(),
                    null,
                    true
            );
        }

        // 2. Enhanced Intent Extraction with LLM + Deterministic Fallback
        LLMIntentExtractor.ExtractedIntent llmExtracted = llmIntentExtractor.extractIntent(
                product.getSku(),
                product.getBasePricePaise(),
                request.buyerMessage()
        );

        // If LLM indicates adversarial injection, block immediately
        if (llmExtracted.adversarialInjection()) {
            return new NegotiateChatResponse(
                    "sess_" + UUID.randomUUID().toString().substring(0, 8),
                    merchant.getMerchantCode(),
                    product.getSku(),
                    "SECURITY_ALERT: The proposal was blocked by the CommerceDNA Deterministic Policy Airgap. Adversarial manipulation or margin override attempt detected.",
                    ProposalStatus.REJECTED,
                    null,
                    null,
                    null,
                    null,
                    llmExtracted.rejectionReason(),
                    null,
                    true
            );
        }

        // Use LLM extraction when confident and valid, otherwise fallback to deterministic
        DualModelIntentCompiler.ExtractedIntent extracted;
        if (llmExtracted.confidence() >= 0.7 && llmExtracted.sku() != null) {
            extracted = new DualModelIntentCompiler.ExtractedIntent(
                    llmExtracted.sku(),
                    llmExtracted.quantity(),
                    llmExtracted.proposedUnitPricePaise(),
                    llmExtracted.intentDetected(),
                    llmExtracted.adversarialInjection(),
                    llmExtracted.rejectionReason()
            );
        } else {
            extracted = deterministicExtracted;
        }

        if (extracted.adversarialInjection()) {
            return new NegotiateChatResponse(
                    "sess_" + UUID.randomUUID().toString().substring(0, 8),
                    merchant.getMerchantCode(),
                    product.getSku(),
                    "SECURITY_ALERT: The proposal was blocked by the CommerceDNA Deterministic Policy Airgap. Adversarial manipulation or margin override attempt detected.",
                    ProposalStatus.REJECTED,
                    null,
                    null,
                    null,
                    null,
                    extracted.rejectionReason(),
                    null,
                    true
            );
        }

        // 2. Submit structured proposal
        SubmitProposalCommand cmd = new SubmitProposalCommand(
                merchant.getMerchantCode(),
                request.buyerAgentDid(),
                request.buyerPublicKey(),
                product.getSku(),
                extracted.quantity() != null ? extracted.quantity() : 1,
                extracted.proposedUnitPricePaise() != null ? extracted.proposedUnitPricePaise() : product.getBasePricePaise(),
                request.buyerSignature()
        );


        ProposalResult result = submitProposal(cmd);

        String agentReply;
        if (result.status() == ProposalStatus.ACCEPTED) {
            agentReply = String.format("Agreement reached for %d unit(s) of %s at %s each (Total: %s). Proposal code '%s' is locked for 15 minutes. Proceed to settlement.",
                    result.quantity(), product.getTitle(),
                    Money.ofPaise(result.proposedUnitPricePaise()).formatInRupees(),
                    Money.ofPaise(result.totalAmountPaise()).formatInRupees(),
                    result.proposalCode()
            );
        } else if (result.status() == ProposalStatus.COUNTERED) {
            agentReply = String.format("We cannot accept %s as it breaches merchant margin floors. However, we counter-offer %d unit(s) at %s each (Total: %s). Proposal code '%s' generated.",
                    Money.ofPaise(result.proposedUnitPricePaise()).formatInRupees(),
                    result.quantity(),
                    Money.ofPaise(result.counterUnitPricePaise()).formatInRupees(),
                    Money.ofPaise(result.totalAmountPaise()).formatInRupees(),
                    result.proposalCode()
            );
        } else {
            agentReply = "Unable to accept proposal. " + result.rationale();
        }

        return new NegotiateChatResponse(
                "sess_" + UUID.randomUUID().toString().substring(0, 8),
                merchant.getMerchantCode(),
                product.getSku(),
                agentReply,
                result.status(),
                result.proposalCode(),
                result.counterUnitPricePaise() != null ? result.counterUnitPricePaise() : result.proposedUnitPricePaise(),
                result.quantity(),
                result.totalAmountPaise(),
                result.rationale(),
                result.merchantSignature(),
                false
        );
    }

    @Transactional(readOnly = true)
    public ProposalEntity getProposalByCode(String proposalCode) {
        return proposalRepository.findByProposalCode(proposalCode)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", proposalCode));
    }

    @Transactional(readOnly = true)
    public ProposalEntity getProposalById(UUID proposalId) {
        return proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal", proposalId.toString()));
    }

    private ProposalResult mapToResult(ProposalEntity entity, String merchantCode) {
        return new ProposalResult(
                entity.getId(),
                entity.getProposalCode(),
                entity.getMerchantId(),
                merchantCode,
                entity.getBuyerAgentDid(),
                entity.getSku(),
                entity.getQuantity(),
                entity.getProposedUnitPricePaise(),
                entity.getCounterUnitPricePaise(),
                entity.getTotalAmountPaise(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getRationale(),
                entity.getBuyerSignature(),
                entity.getMerchantSignature(),
                entity.getExpiresAt(),
                entity.getCreatedAt()
        );
    }
}
