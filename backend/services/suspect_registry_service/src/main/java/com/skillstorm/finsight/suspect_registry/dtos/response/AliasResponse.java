package com.skillstorm.finsight.suspect_registry.dtos.response;

import java.time.Instant;

import com.skillstorm.finsight.suspect_registry.models.AliasType;

public record AliasResponse(
        Long aliasId,
        Long suspectId,
        String aliasName,
        AliasType aliasType,
        Boolean isPrimary,
        Instant createdAt
) {
}
