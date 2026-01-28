package com.skillstorm.finsight.suspect_registry.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank(message = "Organization name is required")
        @Size(max = 256)
        String name,

        @Size(max = 64)
        String type
) {
}
