package com.skillstorm.finsight.documents_cases.dtos.response;

import java.time.Instant;

public record CaseNoteResponse(
        Long noteId,
        Long caseId,
        String authorUserId,
        String noteText,
        Instant createdAt
) {
}
