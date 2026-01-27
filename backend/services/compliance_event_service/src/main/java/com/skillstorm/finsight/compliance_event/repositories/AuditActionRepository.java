package com.skillstorm.finsight.compliance_event.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skillstorm.finsight.compliance_event.models.AuditAction;

@Repository
public interface AuditActionRepository extends JpaRepository<AuditAction, Long> {

    // Audit trail for a specific event
    Page<AuditAction> findByEvent_EventIdOrderByCreatedAtDesc(Long eventId, Pageable pageable);

    List<AuditAction> findTop20ByEvent_EventIdOrderByCreatedAtDesc(Long eventId);

    // "Who did what" views
    Page<AuditAction> findByActorUserIdOrderByCreatedAtDesc(UUID actorUserId, Pageable pageable);

    // Tracing across services
    Page<AuditAction> findByCorrelationIdOrderByCreatedAtDesc(String correlationId, Pageable pageable);

    // Idempotent audit writes (via generated idempotency_key_nn in schema)
    Optional<AuditAction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    // Filter by action type
    Page<AuditAction> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    // Time-window queries (exports)
    Page<AuditAction> findByCreatedAtBetweenOrderByCreatedAtDesc(
            Instant fromInclusive,
            Instant toInclusive,
            Pageable pageable);
}
