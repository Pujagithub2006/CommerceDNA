package io.commercedna.audit.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.commercedna.audit.entity.AuditLedgerEntity;
import io.commercedna.audit.repository.AuditLedgerJpaRepository;
import io.commercedna.audit.scrubber.PiiLogScrubber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * SHA-256 Merkle-Linked Append-Only Cryptographic Audit Ledger Engine.
 * Each block is cryptographically chained to the previous block's SHA-256 hash.
 */
@Service
@Transactional
public class AuditLedgerEngine {

    private static final Logger log = LoggerFactory.getLogger(AuditLedgerEngine.class);
    public static final String GENESIS_PREV_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final AuditLedgerJpaRepository auditRepository;
    private final PiiLogScrubber piiScrubber;
    private final ObjectMapper objectMapper;

    public AuditLedgerEngine(
            AuditLedgerJpaRepository auditRepository,
            PiiLogScrubber piiScrubber,
            ObjectMapper objectMapper
    ) {
        this.auditRepository = Objects.requireNonNull(auditRepository);
        this.piiScrubber = Objects.requireNonNull(piiScrubber);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public synchronized AuditLedgerEntity recordEvent(String eventType, String entityType, String entityId, Object payload) {
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(entityType, "entityType must not be null");
        Objects.requireNonNull(entityId, "entityId must not be null");

        String rawJson;
        try {
            if (payload instanceof String s) {
                rawJson = s;
            } else {
                rawJson = objectMapper.writeValueAsString(payload);
            }
        } catch (Exception ex) {
            rawJson = String.valueOf(payload);
        }

        String sanitizedJson = piiScrubber.scrub(rawJson);
        String payloadHash = calculateSha256(sanitizedJson);

        Optional<AuditLedgerEntity> lastBlock = auditRepository.findTopByOrderBySequenceNumberDesc();
        long nextSequence = lastBlock.map(b -> b.getSequenceNumber() + 1).orElse(1L);
        String previousBlockHash = lastBlock.map(AuditLedgerEntity::getBlockHash).orElse(GENESIS_PREV_HASH);

        Instant now = Instant.now();
        String blockHash = calculateBlockHash(nextSequence, eventType, entityType, entityId, payloadHash, previousBlockHash, now);

        AuditLedgerEntity entity = new AuditLedgerEntity(
                UUID.randomUUID(),
                nextSequence,
                eventType,
                entityType,
                entityId,
                payloadHash,
                previousBlockHash,
                blockHash,
                sanitizedJson,
                now
        );

        auditRepository.save(entity);
        log.info("Appended to Cryptographic Audit Ledger: Block #{} [Hash={}, Event={}]", nextSequence, blockHash, eventType);
        return entity;
    }

    public static String calculateBlockHash(
            long sequenceNumber,
            String eventType,
            String entityType,
            String entityId,
            String payloadHash,
            String previousBlockHash,
            Instant timestamp
    ) {
        String canonical = String.format("%d:%s:%s:%s:%s:%s:%d",
                sequenceNumber,
                eventType,
                entityType,
                entityId,
                payloadHash,
                previousBlockHash,
                timestamp.toEpochMilli()
        );
        return calculateSha256(canonical);
    }

    public static String calculateSha256(String data) {
        if (data == null) data = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
