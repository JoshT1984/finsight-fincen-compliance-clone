package com.skillstorm.finsight.suspect_registry.dtos.response;

import java.time.Instant;

import com.skillstorm.finsight.suspect_registry.models.OrganizationType;

public record LinkedOrganizationResponse(
        Long orgId,
        String name,
        OrganizationType type,
        String role,
        Instant linkedAt
) {
}
