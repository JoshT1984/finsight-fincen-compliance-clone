package com.skillstorm.finsight.documents_cases.dtos.response;

import java.time.Instant;

public record DocumentDownloadUrlResponse(
        Long documentId,
        String fileName,
        String downloadUrl,
        Instant expiresAt
) {
}
