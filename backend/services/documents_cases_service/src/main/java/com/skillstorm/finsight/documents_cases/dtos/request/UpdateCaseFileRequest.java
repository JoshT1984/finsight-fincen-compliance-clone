package com.skillstorm.finsight.documents_cases.dtos.request;

import com.skillstorm.finsight.documents_cases.models.CaseStatus;

public record UpdateCaseFileRequest(
                Long sarId,
                Long ctrId,
                CaseStatus status,
                String referredToAgency) {
}
