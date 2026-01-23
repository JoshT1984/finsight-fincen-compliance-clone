package com.skillstorm.finsight.documents_cases.dtos.response;

import java.time.Instant;
import java.util.UUID;

public record CaseNoteResponse(
        Long noteId,
        Long caseId,
        UUID authorUserId,
        String noteText,
        Instant createdAt
) {
}
