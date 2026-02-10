package com.skillstorm.finsight.compliance_event.services;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.skillstorm.finsight.compliance_event.dtos.ComplianceEventResponse;
import com.skillstorm.finsight.compliance_event.dtos.CreateCtrRequest;
import com.skillstorm.finsight.compliance_event.dtos.CreateSarRequest;
import com.skillstorm.finsight.compliance_event.models.EventStatus;
import com.skillstorm.finsight.compliance_event.models.EventType;

public interface ComplianceEventService {

    ComplianceEventResponse createSar(CreateSarRequest request);

    ComplianceEventResponse createCtr(CreateCtrRequest request);

    ComplianceEventResponse getById(Long eventId);

    Page<ComplianceEventResponse> search(
            EventType eventType,
            EventStatus status,
            String sourceSystem,
            Instant from,
            Instant to,
            Pageable pageable);

    /** Find compliance events (CTRs, SARs) linked to the given suspect. */
    Page<ComplianceEventResponse> findBySuspectId(Long suspectId, Pageable pageable);

    /** Find events of given type that can be linked to the given suspect (not already linked to them). */
    Page<ComplianceEventResponse> findLinkableByEventType(EventType eventType, Long excludeSuspectId, Pageable pageable);

    /** Link a compliance event (CTR or SAR) to a suspect. */
    ComplianceEventResponse linkEventToSuspect(Long eventId, Long suspectId);

    /** Remove the suspect link from a compliance event. */
    ComplianceEventResponse unlinkEventFromSuspect(Long eventId);
}
