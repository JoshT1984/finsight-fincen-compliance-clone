package com.skillstorm.finsight.suspect_registry.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateSuspectRequest(
        @NotBlank(message = "Primary name is required")
        @Size(max = 256)
        String primaryName,

        LocalDate dob,

        @Size(max = 4)
        String ssnLast4,

        @Size(max = 16)
        String riskLevel
) {
}
