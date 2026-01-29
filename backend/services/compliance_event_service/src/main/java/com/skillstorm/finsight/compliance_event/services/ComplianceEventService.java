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
}
