package com.skillstorm.finsight.suspect_registry.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FindOrCreateBySsnRequest(
        @NotBlank @Size(max = 11) String ssn,
        @Size(max = 256) String primaryName) {
}
