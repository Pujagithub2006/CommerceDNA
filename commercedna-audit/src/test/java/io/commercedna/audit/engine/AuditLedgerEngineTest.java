package io.commercedna.audit.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.commercedna.audit.entity.AuditLedgerEntity;
import io.commercedna.audit.repository.AuditLedgerJpaRepository;
import io.commercedna.audit.scrubber.PiiLogScrubber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLedgerEngineTest {

    @Mock
    private AuditLedgerJpaRepository auditRepository;

    private PiiLogScrubber piiScrubber;
    private ObjectMapper objectMapper;
    private AuditLedgerEngine engine;

    @BeforeEach
    void setUp() {
        piiScrubber = new PiiLogScrubber();
        objectMapper = new ObjectMapper().findAndRegisterModules();
        engine = new AuditLedgerEngine(auditRepository, piiScrubber, objectMapper);
    }

    @Test
    @DisplayName("Should create Genesis Block #1 with zero hash when ledger is empty")
    void shouldCreateGenesisBlock() {
        when(auditRepository.findTopByOrderBySequenceNumberDesc()).thenReturn(Optional.empty());

        AuditLedgerEntity block = engine.recordEvent("MERCHANT_REGISTERED", "MERCHANT", "m_001", Map.of("code", "apex-tech"));

        assertEquals(1L, block.getSequenceNumber());
        assertEquals("MERCHANT_REGISTERED", block.getEventType());
        assertEquals("MERCHANT", block.getEntityType());
        assertEquals("m_001", block.getEntityId());
        assertEquals(AuditLedgerEngine.GENESIS_PREV_HASH, block.getPreviousBlockHash());
        assertNotNull(block.getBlockHash());
        assertNotNull(block.getPayloadHash());

        verify(auditRepository, times(1)).save(any(AuditLedgerEntity.class));
    }
}
