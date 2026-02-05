package com.skillstorm.finsight.documents_cases.dtos.response;

import java.time.Instant;

import com.skillstorm.finsight.documents_cases.models.DocumentType;

public record DocumentResponse(
        Long documentId,
        DocumentType documentType,
        String fileName,
        String storagePath,
        Instant uploadedAt,
        Long ctrId,
        Long sarId,
        Long caseId,
        String uploadedByUserId
) {
}
