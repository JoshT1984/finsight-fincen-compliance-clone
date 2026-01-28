package com.skillstorm.finsight.suspect_registry.dtos.response;

import java.time.Instant;

public record OrganizationResponse(
        Long orgId,
        String name,
        String type,
        Instant createdAt
) {
}
