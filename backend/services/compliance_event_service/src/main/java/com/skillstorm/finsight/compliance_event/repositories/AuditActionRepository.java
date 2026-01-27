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

    Page<AuditAction> findByEvent_EventIdOrderByCreatedAtDesc(Long eventId, Pageable pageable);

    List<AuditAction> findTop20ByEvent_EventIdOrderByCreatedAtDesc(Long eventId);

    Page<AuditAction> findByActorUserIdOrderByCreatedAtDesc(UUID actorUserId, Pageable pageable);

    Page<AuditAction> findByCorrelationIdOrderByCreatedAtDesc(String correlationId, Pageable pageable);

    Optional<AuditAction> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Page<AuditAction> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    Page<AuditAction> findByCreatedAtBetweenOrderByCreatedAtDesc(
            Instant fromInclusive,
            Instant toInclusive,
            Pageable pageable);
}
