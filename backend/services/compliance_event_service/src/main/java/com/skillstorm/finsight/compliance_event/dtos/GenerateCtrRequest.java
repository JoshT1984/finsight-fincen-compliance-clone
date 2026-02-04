package com.skillstorm.finsight.compliance_event.dtos;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record GenerateCtrRequest(
    @NotNull LocalDate from,
    @NotNull LocalDate to
) {}

