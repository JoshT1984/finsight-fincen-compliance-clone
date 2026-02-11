package com.skillstorm.finsight.documents_cases.dtos.compliance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Payload for POST /api/compliance-events/ctr.
 * Mirrors compliance_event_service CreateCtrRequest for JSON compatibility.
 */
public record CreateCtrRequestPayload(
        String sourceSystem,
        String sourceEntityId,
        String externalSubjectKey,
        Instant eventTime,
        BigDecimal totalAmount,
        String customerName,
        Instant transactionTime,
        Map<String, Object> ctrFormData) {
}
