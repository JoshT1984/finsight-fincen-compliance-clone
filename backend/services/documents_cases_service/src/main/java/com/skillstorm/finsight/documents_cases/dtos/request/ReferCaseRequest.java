package com.skillstorm.finsight.documents_cases.dtos.request;

import jakarta.validation.constraints.NotBlank;

public record ReferCaseRequest(
        @NotBlank(message = "Referred to agency is required")
        String referredToAgency
) {
}
