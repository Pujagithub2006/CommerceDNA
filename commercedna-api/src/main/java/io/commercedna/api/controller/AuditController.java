package io.commercedna.api.controller;

import io.commercedna.audit.engine.ChainIntegrityVerifier;
import io.commercedna.audit.entity.AuditLedgerEntity;
import io.commercedna.audit.repository.AuditLedgerJpaRepository;
import io.commercedna.catalog.repository.ProductJpaRepository;
import io.commercedna.identity.repository.MerchantJpaRepository;
import io.commercedna.negotiation.repository.ProposalJpaRepository;
import io.commercedna.settlement.repository.OrderJpaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Audit & Analytics", description = "Cryptographic Merkle audit ledger, chain integrity verification, and merchant analytics")
public class AuditController {

    private final AuditLedgerJpaRepository auditRepository;
    private final ChainIntegrityVerifier chainIntegrityVerifier;
    private final MerchantJpaRepository merchantRepository;
    private final ProductJpaRepository productRepository;
    private final OrderJpaRepository orderRepository;
    private final ProposalJpaRepository proposalRepository;

    public AuditController(
            AuditLedgerJpaRepository auditRepository,
            ChainIntegrityVerifier chainIntegrityVerifier,
            MerchantJpaRepository merchantRepository,
            ProductJpaRepository productRepository,
            OrderJpaRepository orderRepository,
            ProposalJpaRepository proposalRepository
    ) {
        this.auditRepository = Objects.requireNonNull(auditRepository);
        this.chainIntegrityVerifier = Objects.requireNonNull(chainIntegrityVerifier);
        this.merchantRepository = Objects.requireNonNull(merchantRepository);
        this.productRepository = Objects.requireNonNull(productRepository);
        this.orderRepository = Objects.requireNonNull(orderRepository);
        this.proposalRepository = Objects.requireNonNull(proposalRepository);
    }

    @GetMapping("/audit/ledger")
    @Operation(summary = "Fetch the latest immutable SHA-256 Merkle-linked audit ledger records")
    public ResponseEntity<List<AuditLedgerEntity>> getRecentLedgerRecords(
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        List<AuditLedgerEntity> blocks = auditRepository.findRecentBlocks(PageRequest.of(0, Math.min(limit, 100)));
        return ResponseEntity.ok(blocks);
    }

    @PostMapping("/audit/verify")
    @Operation(summary = "Perform cryptographic proof verification across the entire Merkle chain from genesis")
    public ResponseEntity<ChainIntegrityVerifier.ChainVerificationResult> verifyLedgerIntegrity() {
        ChainIntegrityVerifier.ChainVerificationResult result = chainIntegrityVerifier.verifyChainIntegrity();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/analytics/overview")
    @Operation(summary = "Fetch high-speed real-time platform statistics, GMV, and protected margin metrics")
    public ResponseEntity<Map<String, Object>> getAnalyticsOverview() {
        long merchantCount = merchantRepository.count();
        long productCount = productRepository.count();
        long orderCount = orderRepository.count();
        long proposalCount = proposalRepository.count();
        long auditBlockCount = auditRepository.count();

        long totalGmvPaise = orderRepository.findAll().stream()
                .mapToLong(io.commercedna.settlement.entity.OrderEntity::getTotalAmountPaise)
                .sum();

        return ResponseEntity.ok(Map.of(
                "totalMerchants", merchantCount,
                "totalProducts", productCount,
                "totalOrders", orderCount,
                "totalProposals", proposalCount,
                "auditLedgerBlocks", auditBlockCount,
                "totalGmvPaise", totalGmvPaise,
                "currency", "INR",
                "marginProtectionStatus", "ACTIVE"
        ));
    }
}
