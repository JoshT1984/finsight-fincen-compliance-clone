package com.skillstorm.finsight.documents_cases.dtos.response;

import com.skillstorm.finsight.documents_cases.models.CaseStatus;
import java.time.Instant;

public record CaseFileResponse(
        Long caseId,
        Long sarId,
        CaseStatus status,
        Instant createdAt,
        Instant referredAt,
        Instant closedAt,
        String referredToAgency
) {
}
