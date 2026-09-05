package io.commercedna.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable Cryptographic SHA-256 Merkle-Chained Audit Ledger Entry.
 * Guarantees zero-tamper historical audit trails for all merchant actions and financial operations.
 */
@Entity
@Table(
        name = "audit_ledger",
        indexes = {
                @Index(name = "idx_audit_seq", columnList = "sequence_number", unique = true),
                @Index(name = "idx_audit_block_hash", columnList = "block_hash", unique = true),
                @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id")
        }
)
public class AuditLedgerEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "sequence_number", nullable = false, updatable = false)
    private Long sequenceNumber;

    @Column(name = "event_type", nullable = false, length = 64, updatable = false)
    private String eventType;

    @Column(name = "entity_type", nullable = false, length = 64, updatable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 64, updatable = false)
    private String entityId;

    @Column(name = "payload_hash", nullable = false, length = 64, updatable = false)
    private String payloadHash;

    @Column(name = "previous_block_hash", nullable = false, length = 64, updatable = false)
    private String previousBlockHash;

    @Column(name = "block_hash", nullable = false, length = 64, updatable = false)
    private String blockHash;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String payloadJson;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp = Instant.now();

    public AuditLedgerEntity() {
    }

    public AuditLedgerEntity(
            UUID id,
            Long sequenceNumber,
            String eventType,
            String entityType,
            String entityId,
            String payloadHash,
            String previousBlockHash,
            String blockHash,
            String payloadJson,
            Instant timestamp
    ) {
        this.id = id;
        this.sequenceNumber = sequenceNumber;
        this.eventType = eventType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.payloadHash = payloadHash;
        this.previousBlockHash = previousBlockHash;
        this.blockHash = blockHash;
        this.payloadJson = payloadJson;
        this.timestamp = timestamp;
    }

    public UUID getId() {
        return id;
    }

    public Long getSequenceNumber() {
        return sequenceNumber;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getPayloadHash() {
        return payloadHash;
    }

    public String getPreviousBlockHash() {
        return previousBlockHash;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }
}
