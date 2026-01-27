package com.skillstorm.finsight.compliance_event.repositories;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skillstorm.finsight.compliance_event.models.SuspectSnapshotAtTimeOfEvent;

@Repository
public interface SuspectSnapshotAtTimeOfEventRepository extends JpaRepository<SuspectSnapshotAtTimeOfEvent, Long> {

    List<SuspectSnapshotAtTimeOfEvent> findBySuspectIdOrderByCapturedAtDesc(Long suspectId);

    List<SuspectSnapshotAtTimeOfEvent> findBySuspectId(Long suspectId, Pageable pageable);

    default SuspectSnapshotAtTimeOfEvent findLatestForSuspect(Long suspectId) {
        List<SuspectSnapshotAtTimeOfEvent> results = findBySuspectId(suspectId, Pageable.ofSize(1));
        return results.isEmpty() ? null : results.get(0);
    }
}
