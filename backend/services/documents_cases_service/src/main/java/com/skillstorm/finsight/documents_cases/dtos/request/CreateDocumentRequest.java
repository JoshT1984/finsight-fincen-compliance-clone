package com.skillstorm.finsight.documents_cases.dtos.request;

import com.skillstorm.finsight.documents_cases.models.DocumentType;
import jakarta.validation.constraints.NotNull;

public record CreateDocumentRequest(
        @NotNull(message = "Document type is required")
        DocumentType documentType,
        
        @NotNull(message = "File name is required")
        String fileName,
        
        @NotNull(message = "Storage path is required")
        String storagePath,
        
        Long ctrId,
        Long sarId,
        Long caseId
) {
}
