package com.skillstorm.finsight.documents_cases.dtos;

import java.time.Instant;

import com.skillstorm.finsight.documents_cases.models.DocumentType;

/**
 * Event payload published to RabbitMQ when a CTR or SAR document is uploaded.
 * Compliance event service consumes this to create/update CTR/SAR records.
 *
 * <p>The consumer can access the document via:
 * <ul>
 *   <li>{@code downloadUrl} – presigned S3 URL for direct HTTP download (expires in ~60 min)</li>
 *   <li>{@code bucketName} + {@code storagePath} – for consumers with S3 access</li>
 *   <li>Documents API: {@code GET /api/documents/{documentId}/download-url} – for a fresh presigned URL</li>
 * </ul>
 */
public record DocumentUploadEvent(
    Long documentId,
    DocumentType documentType,
    Long ctrId,
    Long sarId,
    Long caseId,
    String fileName,
    String storagePath,
    String bucketName,
    /** Presigned S3 URL for direct download. Null if generation failed. Expires in ~60 minutes. */
    String downloadUrl,
    Instant uploadedAt
) {}
