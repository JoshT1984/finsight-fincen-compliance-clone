package com.skillstorm.finsight.compliance_event.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.skillstorm.finsight.compliance_event.models.ComplianceEvent;

public record ComplianceEventResponse(
        Long eventId,
        ComplianceEvent.EventType eventType,
        String sourceSystem,
        String sourceEntityId,
        OffsetDateTime eventTime,
        BigDecimal totalAmount,
        String status,
        Integer severityScore) {
}