package com.skillstorm.finsight.suspect_registry.dtos.request;

import com.skillstorm.finsight.suspect_registry.models.AliasType;

import jakarta.validation.constraints.Size;

public record PatchAliasRequest(
        Long suspectId,

        @Size(max = 256)
        String aliasName,

        AliasType aliasType,

        Boolean isPrimary
) {
}
