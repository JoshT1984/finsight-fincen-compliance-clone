package com.skillstorm.finsight.suspect_registry.dtos.request;

import java.time.LocalDate;

import com.skillstorm.finsight.suspect_registry.models.RiskLevel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSuspectRequest(
        @NotBlank(message = "Primary name is required")
        @Size(max = 256)
        String primaryName,

        LocalDate dob,

        @Size(max = 11, message = "SSN must contain exactly 9 digits (e.g., 123-45-6789 or 123456789)")
        String ssn,

        RiskLevel riskLevel
) {
}
