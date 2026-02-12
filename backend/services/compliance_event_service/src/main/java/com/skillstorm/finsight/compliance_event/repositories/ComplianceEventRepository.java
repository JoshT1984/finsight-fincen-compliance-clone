
package com.skillstorm.finsight.compliance_event.repositories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.skillstorm.finsight.compliance_event.models.ComplianceEvent;
import com.skillstorm.finsight.compliance_event.models.EventStatus;
import com.skillstorm.finsight.compliance_event.models.EventType;

@Repository
public interface ComplianceEventRepository extends JpaRepository<ComplianceEvent, Long> {

        Page<ComplianceEvent> findByEventType(EventType eventType, Pageable pageable);

        Page<ComplianceEvent> findByStatus(EventStatus status, Pageable pageable);

        Page<ComplianceEvent> findBySourceSystemAndSourceEntityId(
                        String sourceSystem,
                        String sourceEntityId,
                        Pageable pageable);

        Page<ComplianceEvent> findByEventTimeBetween(
                        Instant startInclusive,
                        Instant endInclusive,
                        Pageable pageable);

        @Query("""
                        SELECT e
                        FROM ComplianceEvent e
                        WHERE (:eventType IS NULL OR e.eventType = :eventType)
                          AND (:status IS NULL OR e.status = :status)
                          AND (:sourceSystem IS NULL OR e.sourceSystem = :sourceSystem)
                          AND (:from IS NULL OR e.eventTime >= :from)
                          AND (:to IS NULL OR e.eventTime <= :to)
                        """)
        Page<ComplianceEvent> search(
                        @Param("eventType") EventType eventType,
                        @Param("status") EventStatus status,
                        @Param("sourceSystem") String sourceSystem,
                        @Param("from") Instant from,
                        @Param("to") Instant to,
                        Pageable pageable);

        Optional<ComplianceEvent> findByIdempotencyKey(String idempotencyKey);

        Optional<ComplianceEvent> findByIdempotencyKeyAndEventType(String idempotencyKey, EventType eventType);

        Page<ComplianceEvent> findBySuspectSnapshot_SuspectId(
                        Long suspectId,
                        Pageable pageable);

        @Query("""
                         SELECT e FROM ComplianceEvent e
                        WHERE e.eventType = :eventType
                           AND (e.suspectSnapshot IS NULL
                                OR e.suspectSnapshot.suspectId <> :excludeSuspectId)
                         ORDER BY e.eventTime DESC
                         """)
        Page<ComplianceEvent> findLinkableByEventType(
                        @Param("eventType") EventType eventType,
                        @Param("excludeSuspectId") Long excludeSuspectId,
                        Pageable pageable);

        // ✅ CTR detail support
        List<ComplianceEvent> findByExternalSubjectKeyAndEventTypeOrderByEventTimeDesc(
                        String externalSubjectKey,
                        EventType eventType);

        /**
         * ✅ Deterministic CTR idempotency check:
         * Find an existing CTR for the same subjectKey within a UTC day window
         * [dayStart, dayEnd).
         */
        @Query("""
                        SELECT e
                        FROM ComplianceEvent e
                        WHERE e.eventType = :eventType
                          AND e.externalSubjectKey = :subjectKey
                          AND e.eventTime >= :dayStart
                          AND e.eventTime < :dayEnd
                        """)
        Optional<ComplianceEvent> findCtrForSubjectDay(
                        @Param("eventType") EventType eventType,
                        @Param("subjectKey") String subjectKey,
                        @Param("dayStart") Instant dayStart,
                        @Param("dayEnd") Instant dayEnd);
}
