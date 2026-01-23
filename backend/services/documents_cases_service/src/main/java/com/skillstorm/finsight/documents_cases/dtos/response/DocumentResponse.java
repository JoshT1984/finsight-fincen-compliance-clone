package com.skillstorm.finsight.documents_cases.dtos.response;

import com.skillstorm.finsight.documents_cases.models.DocumentType;
import java.time.Instant;

public record DocumentResponse(
        Long documentId,
        DocumentType documentType,
        String fileName,
        String storagePath,
        Instant uploadedAt,
        Long ctrId,
        Long sarId,
        Long caseId
) {
}
