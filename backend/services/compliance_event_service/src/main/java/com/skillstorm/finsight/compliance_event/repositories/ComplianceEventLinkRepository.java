package com.skillstorm.finsight.compliance_event.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.skillstorm.finsight.compliance_event.models.ComplianceEventLink;
import com.skillstorm.finsight.compliance_event.models.ComplianceEventLinkId;

@Repository
public interface ComplianceEventLinkRepository extends JpaRepository<ComplianceEventLink, ComplianceEventLinkId> {

    List<ComplianceEventLink> findByFromEvent_EventId(Long fromEventId);

    List<ComplianceEventLink> findByToEvent_EventId(Long toEventId);

    List<ComplianceEventLink> findById_LinkType(ComplianceEventLink.LinkType linkType);

    boolean existsById_FromEventIdAndId_ToEventIdAndId_LinkType(
            Long fromEventId,
            Long toEventId,
            ComplianceEventLink.LinkType linkType);
}
