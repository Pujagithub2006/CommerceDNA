package io.commercedna.audit.engine;

import io.commercedna.audit.entity.AuditLedgerEntity;
import io.commercedna.audit.repository.AuditLedgerJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Automated Cryptographic Audit Ledger Chain Integrity Verifier.
 * Performs linear traversal and mathematical verification of all SHA-256 Merkle links.
 */
@Service
@Transactional(readOnly = true)
public class ChainIntegrityVerifier {

    private static final Logger log = LoggerFactory.getLogger(ChainIntegrityVerifier.class);

    private final AuditLedgerJpaRepository auditRepository;

    public ChainIntegrityVerifier(AuditLedgerJpaRepository auditRepository) {
        this.auditRepository = Objects.requireNonNull(auditRepository);
    }

    public record ChainVerificationResult(
            boolean intact,
            long totalBlocksVerified,
            Long corruptedSequenceNumber,
            String message
    ) {}

    public ChainVerificationResult verifyChainIntegrity() {
        List<AuditLedgerEntity> blocks = auditRepository.findAllByOrderBySequenceNumberAsc();
        if (blocks.isEmpty()) {
            return new ChainVerificationResult(true, 0, null, "Audit ledger is empty (Genesis state).");
        }

        String expectedPreviousHash = AuditLedgerEngine.GENESIS_PREV_HASH;
        long expectedSequence = 1L;

        for (AuditLedgerEntity block : blocks) {
            // 1. Verify Sequence Continuity
            if (block.getSequenceNumber() != expectedSequence) {
                String error = String.format("Sequence broken at block ID %s: Expected sequence #%d, but found #%d.",
                        block.getId(), expectedSequence, block.getSequenceNumber());
                log.error(error);
                return new ChainVerificationResult(false, expectedSequence - 1, block.getSequenceNumber(), error);
            }

            // 2. Verify Previous Block Hash Link
            if (!Objects.equals(block.getPreviousBlockHash(), expectedPreviousHash)) {
                String error = String.format("Hash link broken at Block #%d: Expected prevHash '%s', but found '%s'.",
                        block.getSequenceNumber(), expectedPreviousHash, block.getPreviousBlockHash());
                log.error(error);
                return new ChainVerificationResult(false, expectedSequence - 1, block.getSequenceNumber(), error);
            }

            // 3. Verify Payload SHA-256 Hash
            String recalculatedPayloadHash = AuditLedgerEngine.calculateSha256(block.getPayloadJson());
            if (!Objects.equals(block.getPayloadHash(), recalculatedPayloadHash)) {
                String error = String.format("Payload tampered at Block #%d: Stored hash '%s' does not match computed hash '%s'.",
                        block.getSequenceNumber(), block.getPayloadHash(), recalculatedPayloadHash);
                log.error(error);
                return new ChainVerificationResult(false, expectedSequence - 1, block.getSequenceNumber(), error);
            }

            // 4. Verify Block SHA-256 Hash
            String recalculatedBlockHash = AuditLedgerEngine.calculateBlockHash(
                    block.getSequenceNumber(),
                    block.getEventType(),
                    block.getEntityType(),
                    block.getEntityId(),
                    block.getPayloadHash(),
                    block.getPreviousBlockHash(),
                    block.getTimestamp()
            );

            if (!Objects.equals(block.getBlockHash(), recalculatedBlockHash)) {
                String error = String.format("Block hash signature invalid at Block #%d: Stored '%s', Computed '%s'.",
                        block.getSequenceNumber(), block.getBlockHash(), recalculatedBlockHash);
                log.error(error);
                return new ChainVerificationResult(false, expectedSequence - 1, block.getSequenceNumber(), error);
            }

            expectedPreviousHash = block.getBlockHash();
            expectedSequence++;
        }

        log.info("Cryptographic audit chain verified successfully: {} blocks checked, ZERO tamper detected.", blocks.size());
        return new ChainVerificationResult(
                true,
                blocks.size(),
                null,
                String.format("Cryptographic chain verified: %d blocks intact. Zero corruption detected.", blocks.size())
        );
    }
}
