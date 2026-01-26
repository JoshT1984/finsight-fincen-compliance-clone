package com.skillstorm.finsight.documents_cases.dtos.request;

import jakarta.validation.constraints.NotNull;

public record CreateCaseNoteRequest(
        @NotNull(message = "Case ID is required")
        Long caseId,
        
        @NotNull(message = "Author user ID is required")
        String authorUserId,
        
        @NotNull(message = "Note text is required")
        String noteText
) {
}
