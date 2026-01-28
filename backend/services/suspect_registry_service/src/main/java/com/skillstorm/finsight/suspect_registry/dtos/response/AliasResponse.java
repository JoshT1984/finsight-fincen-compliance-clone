package com.skillstorm.finsight.suspect_registry.dtos.response;

import java.time.Instant;

public record AliasResponse(
        Long aliasId,
        Long suspectId,
        String aliasName,
        String aliasType,
        Boolean isPrimary,
        Instant createdAt
) {
}
