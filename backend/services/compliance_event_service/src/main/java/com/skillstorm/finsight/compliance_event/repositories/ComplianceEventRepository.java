package com.skillstorm.finsight.compliance_event.repositories;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.skillstorm.finsight.compliance_event.models.ComplianceEvent;

@Repository
public interface ComplianceEventRepository extends JpaRepository<ComplianceEvent, Long> {

        Page<ComplianceEvent> findByEventType(ComplianceEvent.EventType eventType, Pageable pageable);

        Page<ComplianceEvent> findByStatus(ComplianceEvent.ComplianceEventStatus status, Pageable pageable);

        Page<ComplianceEvent> findBySourceSystemAndSourceEntityId(String sourceSystem, String sourceEntityId,
                        Pageable pageable);

        Page<ComplianceEvent> findByEventTimeBetween(Instant startInclusive, Instant endInclusive, Pageable pageable);

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
                        @Param("eventType") ComplianceEvent.EventType eventType,
                        @Param("status") ComplianceEvent.ComplianceEventStatus status,
                        @Param("sourceSystem") String sourceSystem,
                        @Param("from") Instant from,
                        @Param("to") Instant to,
                        Pageable pageable);
}
