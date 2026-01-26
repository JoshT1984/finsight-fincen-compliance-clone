package com.skillstorm.finsight.documents_cases.dtos.request;

import com.skillstorm.finsight.documents_cases.models.CaseStatus;
import jakarta.validation.constraints.NotNull;

public record CreateCaseFileRequest(
        @NotNull(message = "SAR ID is required")
        Long sarId,
        
        CaseStatus status,
        
        String referredToAgency
) {
}
