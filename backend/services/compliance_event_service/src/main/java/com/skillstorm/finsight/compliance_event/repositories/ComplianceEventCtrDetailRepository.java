package com.skillstorm.finsight.compliance_event.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skillstorm.finsight.compliance_event.models.ComplianceEventCtrDetail;

@Repository
public interface ComplianceEventCtrDetailRepository extends JpaRepository<ComplianceEventCtrDetail, Long> {

    Optional<ComplianceEventCtrDetail> findByEventId(Long eventId);
}
