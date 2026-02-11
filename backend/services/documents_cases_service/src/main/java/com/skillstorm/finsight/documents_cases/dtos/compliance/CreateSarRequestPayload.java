package com.skillstorm.finsight.documents_cases.dtos.compliance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Payload for POST /api/compliance-events/sar.
 * Mirrors compliance_event_service CreateSarRequest for JSON compatibility.
 */
public record CreateSarRequestPayload(
        String sourceSystem,
        String sourceEntityId,
        String externalSubjectKey,
        Instant eventTime,
        BigDecimal totalAmount,
        String narrative,
        Instant activityStart,
        Instant activityEnd,
        Map<String, Object> formData,
        Integer severityScore) {
}
