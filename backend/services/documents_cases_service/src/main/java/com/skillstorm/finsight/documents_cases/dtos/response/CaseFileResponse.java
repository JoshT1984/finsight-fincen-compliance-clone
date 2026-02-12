package com.skillstorm.finsight.documents_cases.dtos.response;

import java.time.Instant;

import com.skillstorm.finsight.documents_cases.models.CaseStatus;

public record CaseFileResponse(
                Long caseId,
                Long sarId,
                Long ctrId,
                CaseStatus status,
                Instant createdAt,
                Instant referredAt,
                Instant closedAt,
                String referredToAgency) {
}