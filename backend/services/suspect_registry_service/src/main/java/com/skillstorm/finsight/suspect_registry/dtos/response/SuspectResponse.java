package com.skillstorm.finsight.suspect_registry.dtos.response;

import java.time.Instant;
import java.time.LocalDate;

public record SuspectResponse(
        Long suspectId,
        String primaryName,
        LocalDate dob,
        String ssnLast4,
        String riskLevel,
        Instant createdAt,
        Instant updatedAt
) {
}
