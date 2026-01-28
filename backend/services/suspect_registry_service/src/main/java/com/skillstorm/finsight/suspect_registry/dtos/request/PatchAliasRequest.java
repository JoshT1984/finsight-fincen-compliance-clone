package com.skillstorm.finsight.suspect_registry.dtos.request;

import jakarta.validation.constraints.Size;

public record PatchAliasRequest(
        Long suspectId,

        @Size(max = 256)
        String aliasName,

        @Size(max = 32)
        String aliasType,

        Boolean isPrimary
) {
}
