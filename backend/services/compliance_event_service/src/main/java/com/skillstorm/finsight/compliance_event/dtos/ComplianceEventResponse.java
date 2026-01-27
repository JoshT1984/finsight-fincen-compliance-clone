package com.skillstorm.finsight.compliance_event.dtos;

import java.math.BigDecimal;
import java.time.Instant;

import com.skillstorm.finsight.compliance_event.models.ComplianceEvent;

public record ComplianceEventResponse(
                Long eventId,
                ComplianceEvent.EventType eventType,
                String sourceSystem,
                String sourceEntityId,
                Instant eventTime,
                BigDecimal totalAmount,
                ComplianceEvent.ComplianceEventStatus status,
                Integer severityScore,
                Instant createdAt) {
}
