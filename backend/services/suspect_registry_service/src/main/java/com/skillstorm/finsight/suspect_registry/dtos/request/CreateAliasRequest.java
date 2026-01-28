package com.skillstorm.finsight.suspect_registry.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAliasRequest(
        @NotNull(message = "Suspect ID is required")
        Long suspectId,

        @NotBlank(message = "Alias name is required")
        @Size(max = 256)
        String aliasName,

        @Size(max = 32)
        String aliasType,

        Boolean isPrimary
) {
}
