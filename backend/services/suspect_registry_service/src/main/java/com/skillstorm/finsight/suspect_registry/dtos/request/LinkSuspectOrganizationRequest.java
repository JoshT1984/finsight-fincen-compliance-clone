package com.skillstorm.finsight.suspect_registry.dtos.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LinkSuspectOrganizationRequest(
        @NotNull(message = "Organization ID is required")
        Long orgId,

        @Size(max = 64)
        String role
) {
}
