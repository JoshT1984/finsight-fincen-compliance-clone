package com.skillstorm.finsight.compliance_event.loggers;

import java.time.Instant;
import java.util.Map;

public record ComplianceEventLog(
        Instant timestamp,
        String entityType,
        String entityId,
        String eventType, // CREATED, UPDATED, FILED
        String trigger, // SYSTEM, USER, SCHEDULED
        String rule,
        String idempotencyKey,
        Map<String, Object> metadata) {
    public static ComplianceEventLog ctrCreated(
            String ctrEventId,
            String idempotencyKey,
            String rule,
            Map<String, Object> metadata) {
        return new ComplianceEventLog(
                Instant.now(),
                "CTR",
                ctrEventId,
                "CREATED",
                "SYSTEM",
                rule,
                idempotencyKey,
                metadata);
    }
}