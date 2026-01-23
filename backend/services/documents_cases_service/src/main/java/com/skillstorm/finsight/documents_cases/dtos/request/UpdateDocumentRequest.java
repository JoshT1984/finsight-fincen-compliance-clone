package com.skillstorm.finsight.documents_cases.dtos.request;

import com.skillstorm.finsight.documents_cases.models.DocumentType;

public record UpdateDocumentRequest(
        DocumentType documentType,
        String fileName,
        String storagePath
) {
}
