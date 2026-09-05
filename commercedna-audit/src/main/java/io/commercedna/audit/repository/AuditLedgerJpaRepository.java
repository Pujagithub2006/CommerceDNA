package io.commercedna.audit.repository;

import io.commercedna.audit.entity.AuditLedgerEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditLedgerJpaRepository extends JpaRepository<AuditLedgerEntity, UUID> {

    Optional<AuditLedgerEntity> findTopByOrderBySequenceNumberDesc();

    List<AuditLedgerEntity> findAllByOrderBySequenceNumberAsc();

    @Query("SELECT e FROM AuditLedgerEntity e ORDER BY e.sequenceNumber DESC")
    List<AuditLedgerEntity> findRecentBlocks(Pageable pageable);

    List<AuditLedgerEntity> findByEntityTypeAndEntityId(String entityType, String entityId);

    Optional<AuditLedgerEntity> findBySequenceNumber(Long sequenceNumber);
}
