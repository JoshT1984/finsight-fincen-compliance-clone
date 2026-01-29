package com.skillstorm.finsight.suspect_registry.dtos.request;

import com.skillstorm.finsight.suspect_registry.models.OrganizationType;

import jakarta.validation.constraints.Size;

public record PatchOrganizationRequest(
        @Size(max = 256)
        String name,

        OrganizationType type
) {
}
