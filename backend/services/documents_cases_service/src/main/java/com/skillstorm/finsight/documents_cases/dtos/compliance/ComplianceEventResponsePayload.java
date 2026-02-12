package com.skillstorm.finsight.documents_cases.dtos.compliance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response from POST /api/compliance-events/ctr or /sar.
 * We only need eventId; other fields are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ComplianceEventResponsePayload(Long eventId) {
}
