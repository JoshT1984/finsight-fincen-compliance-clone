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

@Repository
public interface ComplianceEventRepository extends JpaRepository<ComplianceEvent, Long> {

    Optional<ComplianceEvent> findBySourceSystemAndSourceEntityId(String sourceSystem, String sourceEntityId);

    boolean existsBySourceSystemAndSourceEntityId(String sourceSystem, String sourceEntityId);

    Page<ComplianceEvent> findByEventType(ComplianceEvent.EventType eventType, Pageable pageable);

    Page<ComplianceEvent> findByStatus(ComplianceEvent.ComplianceEventStatus status, Pageable pageable);

    Page<ComplianceEvent> findByEventTimeBetween(Instant startInclusive, Instant endInclusive,
            Pageable pageable);

    @Query("""
            select ce
            from ComplianceEvent ce
            where (:eventType is null or ce.eventType = :eventType)
              and (:status is null or ce.status = :status)
              and (:sourceSystem is null or ce.sourceSystem = :sourceSystem)
              and (:from is null or ce.eventTime >= :from)
              and (:to is null or ce.eventTime <= :to)
            """)
    Page<ComplianceEvent> search(
            @Param("eventType") ComplianceEvent.EventType eventType,
            @Param("status") ComplianceEvent.ComplianceEventStatus status,
            @Param("sourceSystem") String sourceSystem,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("""
            select ce
            from ComplianceEvent ce
            left join fetch ce.suspectSnapshot ss
            where ce.eventId = :eventId
            """)
    Optional<ComplianceEvent> findByIdWithSuspectSnapshot(@Param("eventId") Long eventId);

    @Query("""
            select ce
            from ComplianceEvent ce
            where ce.suspectSnapshot.snapshotId = :snapshotId
            """)
    List<ComplianceEvent> findBySuspectSnapshotId(@Param("snapshotId") Long snapshotId);
}
