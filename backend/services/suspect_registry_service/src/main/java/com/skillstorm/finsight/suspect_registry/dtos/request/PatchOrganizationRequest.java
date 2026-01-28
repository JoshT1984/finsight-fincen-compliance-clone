package com.skillstorm.finsight.suspect_registry.dtos.request;

import jakarta.validation.constraints.Size;

public record PatchOrganizationRequest(
        @Size(max = 256)
        String name,

        @Size(max = 64)
        String type
) {
}
