package com.skillstorm.finsight.compliance_event.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skillstorm.finsight.compliance_event.models.ComplianceEventSarDetail;

@Repository
public interface ComplianceEventSarDetailRepository extends JpaRepository<ComplianceEventSarDetail, Long> {

    Optional<ComplianceEventSarDetail> findByEventId(Long eventId);
}
