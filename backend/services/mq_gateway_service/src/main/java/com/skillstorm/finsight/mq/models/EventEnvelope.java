package com.skillstorm.finsight.mq.models;

import java.time.Instant;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EventEnvelope(
    @NotBlank String eventType,
    @NotBlank String sourceService,
    @NotBlank String correlationId,
    @NotNull Instant occurredAt,
    Map<String, Object> data
) {}
