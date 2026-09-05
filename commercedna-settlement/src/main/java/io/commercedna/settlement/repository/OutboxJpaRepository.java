package io.commercedna.settlement.repository;

import io.commercedna.settlement.entity.OutboxEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query("SELECT e FROM OutboxEventEntity e WHERE e.status = 'PENDING' ORDER BY e.createdAt ASC")
    List<OutboxEventEntity> findPendingEvents(Pageable pageable);

    List<OutboxEventEntity> findByAggregateTypeAndAggregateId(String aggregateType, String aggregateId);
}
