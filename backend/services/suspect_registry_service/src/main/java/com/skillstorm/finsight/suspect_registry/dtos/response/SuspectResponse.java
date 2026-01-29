package com.skillstorm.finsight.suspect_registry.dtos.response;

import java.time.Instant;
import java.time.LocalDate;

import com.skillstorm.finsight.suspect_registry.models.RiskLevel;

public record SuspectResponse(
        Long suspectId,
        String primaryName,
        LocalDate dob,
        String ssn,
        RiskLevel riskLevel,
        Instant createdAt,
        Instant updatedAt
) {
}
