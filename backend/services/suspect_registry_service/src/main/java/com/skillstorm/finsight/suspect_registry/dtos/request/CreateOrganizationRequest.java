package com.skillstorm.finsight.suspect_registry.dtos.request;

import com.skillstorm.finsight.suspect_registry.models.OrganizationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank(message = "Organization name is required")
        @Size(max = 256)
        String name,

        OrganizationType type
) {
}
