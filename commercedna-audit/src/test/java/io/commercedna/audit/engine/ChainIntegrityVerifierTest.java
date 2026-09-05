package io.commercedna.audit.engine;

import io.commercedna.audit.entity.AuditLedgerEntity;
import io.commercedna.audit.repository.AuditLedgerJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChainIntegrityVerifierTest {

    @Mock
    private AuditLedgerJpaRepository auditRepository;

    private ChainIntegrityVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new ChainIntegrityVerifier(auditRepository);
    }

    @Test
    @DisplayName("Should successfully verify intact 3-block cryptographic chain")
    void shouldVerifyIntactChain() {
        Instant t1 = Instant.now().minusSeconds(30);
        String payload1 = "{\"merchant\": \"apex-tech\"}";
        String payloadHash1 = AuditLedgerEngine.calculateSha256(payload1);
        String blockHash1 = AuditLedgerEngine.calculateBlockHash(1L, "MERCHANT_REGISTERED", "MERCHANT", "m1", payloadHash1, AuditLedgerEngine.GENESIS_PREV_HASH, t1);

        AuditLedgerEntity b1 = new AuditLedgerEntity(UUID.randomUUID(), 1L, "MERCHANT_REGISTERED", "MERCHANT", "m1", payloadHash1, AuditLedgerEngine.GENESIS_PREV_HASH, blockHash1, payload1, t1);

        Instant t2 = Instant.now().minusSeconds(20);
        String payload2 = "{\"sku\": \"AURORA-ANC-001\"}";
        String payloadHash2 = AuditLedgerEngine.calculateSha256(payload2);
        String blockHash2 = AuditLedgerEngine.calculateBlockHash(2L, "PRODUCT_CREATED", "PRODUCT", "p1", payloadHash2, blockHash1, t2);

        AuditLedgerEntity b2 = new AuditLedgerEntity(UUID.randomUUID(), 2L, "PRODUCT_CREATED", "PRODUCT", "p1", payloadHash2, blockHash1, blockHash2, payload2, t2);

        Instant t3 = Instant.now().minusSeconds(10);
        String payload3 = "{\"order\": \"ord_123\", \"amount\": 4400000}";
        String payloadHash3 = AuditLedgerEngine.calculateSha256(payload3);
        String blockHash3 = AuditLedgerEngine.calculateBlockHash(3L, "ORDER_CREATED", "ORDER", "ord_123", payloadHash3, blockHash2, t3);

        AuditLedgerEntity b3 = new AuditLedgerEntity(UUID.randomUUID(), 3L, "ORDER_CREATED", "ORDER", "ord_123", payloadHash3, blockHash2, blockHash3, payload3, t3);

        when(auditRepository.findAllByOrderBySequenceNumberAsc()).thenReturn(List.of(b1, b2, b3));

        ChainIntegrityVerifier.ChainVerificationResult result = verifier.verifyChainIntegrity();

        assertTrue(result.intact());
        assertEquals(3L, result.totalBlocksVerified());
        assertNull(result.corruptedSequenceNumber());
    }

    @Test
    @DisplayName("Should detect tamper if payload in block 2 was modified")
    void shouldDetectTamperedPayload() {
        Instant t1 = Instant.now().minusSeconds(30);
        String payload1 = "{\"merchant\": \"apex-tech\"}";
        String payloadHash1 = AuditLedgerEngine.calculateSha256(payload1);
        String blockHash1 = AuditLedgerEngine.calculateBlockHash(1L, "MERCHANT_REGISTERED", "MERCHANT", "m1", payloadHash1, AuditLedgerEngine.GENESIS_PREV_HASH, t1);
        AuditLedgerEntity b1 = new AuditLedgerEntity(UUID.randomUUID(), 1L, "MERCHANT_REGISTERED", "MERCHANT", "m1", payloadHash1, AuditLedgerEngine.GENESIS_PREV_HASH, blockHash1, payload1, t1);

        Instant t2 = Instant.now().minusSeconds(20);
        String payload2 = "{\"sku\": \"AURORA-ANC-001\"}";
        String payloadHash2 = AuditLedgerEngine.calculateSha256(payload2);
        String blockHash2 = AuditLedgerEngine.calculateBlockHash(2L, "PRODUCT_CREATED", "PRODUCT", "p1", payloadHash2, blockHash1, t2);

        // Attacker modified payloadJson in database directly
        AuditLedgerEntity b2Tampered = new AuditLedgerEntity(UUID.randomUUID(), 2L, "PRODUCT_CREATED", "PRODUCT", "p1", payloadHash2, blockHash1, blockHash2, "{\"sku\": \"TAMPERED-SKU\"}", t2);

        when(auditRepository.findAllByOrderBySequenceNumberAsc()).thenReturn(List.of(b1, b2Tampered));

        ChainIntegrityVerifier.ChainVerificationResult result = verifier.verifyChainIntegrity();

        assertFalse(result.intact());
        assertEquals(2L, result.corruptedSequenceNumber());
        assertTrue(result.message().contains("Payload tampered at Block #2"));
    }
}
