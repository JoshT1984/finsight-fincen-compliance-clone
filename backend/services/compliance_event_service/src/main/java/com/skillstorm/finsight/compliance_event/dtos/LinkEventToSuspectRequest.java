package com.skillstorm.finsight.compliance_event.dtos;

import jakarta.validation.constraints.NotNull;

public record LinkEventToSuspectRequest(@NotNull Long suspectId) {
}
