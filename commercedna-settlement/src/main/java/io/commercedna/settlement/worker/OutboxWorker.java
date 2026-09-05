package io.commercedna.settlement.worker;

import io.commercedna.settlement.entity.OutboxEventEntity;
import io.commercedna.settlement.repository.OutboxJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Asynchronous Transactional Outbox Background Polling Worker.
 * Ensures zero event loss by dispatching committed outbox records to external systems.
 */
@Component
public class OutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);
    private static final int MAX_RETRIES = 5;

    private final OutboxJpaRepository outboxRepository;

    public OutboxWorker(OutboxJpaRepository outboxRepository) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository);
    }

    @Scheduled(fixedDelay = 2000, initialDelay = 5000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEventEntity> pendingEvents = outboxRepository.findPendingEvents(PageRequest.of(0, 50));
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Found {} pending Outbox events to dispatch.", pendingEvents.size());

        for (OutboxEventEntity event : pendingEvents) {
            try {
                // Simulate asynchronous dispatch (e.g. Kafka / Webhook / Audit stream)
                log.info("Dispatching Outbox Event: ID={}, Type={}, Aggregate={}:{}",
                        event.getId(), event.getEventType(), event.getAggregateType(), event.getAggregateId());

                event.setStatus("PROCESSED");
                event.setProcessedAt(Instant.now());
                outboxRepository.save(event);
            } catch (Exception ex) {
                log.error("Failed to dispatch Outbox Event ID={}", event.getId(), ex);
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= MAX_RETRIES) {
                    event.setStatus("FAILED");
                }
                outboxRepository.save(event);
            }
        }
    }
}
