package com.skillstorm.finsight.compliance_event.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import com.skillstorm.finsight.compliance_event.models.EventStatus;
import com.skillstorm.finsight.compliance_event.models.EventType;

public record ComplianceEventResponse(
        Long eventId,
        EventType eventType,
        String sourceSystem,
        String sourceEntityId,
        Instant eventTime,
        BigDecimal totalAmount,
        EventStatus status,
        Integer severityScore,
        Instant createdAt,
        Long suspectId,
        Map<String, Object> suspectMinimal) {
}
