package com.skillstorm.finsight.documents_cases.loggers;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DocumentEventLog(
        Instant timestamp,
        String entityType, // "DOCUMENT", "CASE_FILE", "CASE_NOTE", etc.
        String entityId,
        String eventType, // "CREATED", "UPDATED", "UPLOAD", etc.
        String trigger, // "SYSTEM", "USER", "SCHEDULED"
        String rule, // Optional
        String idempotencyKey,
        Map<String, Object> metadata) {

    public static DocumentEventLog of(String entityType,
            String entityId,
            String eventType,
            String trigger,
            String rule,
            String idempotencyKey,
            Map<String, Object> metadata) {
        return new DocumentEventLog(
                Instant.now(),
                entityType,
                entityId,
                eventType,
                trigger,
                rule,
                idempotencyKey,
                metadata);
    }

    public static DocumentEventLog documentUploaded(String documentId, String trigger, Map<String, Object> metadata) {
        return of("DOCUMENT", documentId, "UPLOAD", trigger, null, UUID.randomUUID().toString(), metadata);
    }

    // public static DocumentEventLog documentUploaded(
    // String documentId,
    // String trigger,
    // String idempotencyKey,
    // Map<String, Object> metadata) {
    // return new DocumentEventLog(
    // Instant.now(),
    // "DOCUMENT",
    // documentId,
    // "UPLOADED",
    // trigger,
    // null,
    // idempotencyKey,
    // metadata);
    // }

    // public static DocumentEventLog pdfExtracted(
    // String documentId,
    // String trigger,
    // String idempotencyKey,
    // Map<String, Object> metadata) {
    // return new DocumentEventLog(
    // Instant.now(),
    // "DOCUMENT",
    // documentId,
    // "EXTRACTED",
    // trigger,
    // null,
    // idempotencyKey,
    // metadata);
    // }

    // public static DocumentEventLog s3Uploaded(
    // String documentId,
    // String trigger,
    // String idempotencyKey,
    // Map<String, Object> metadata) {
    // return new DocumentEventLog(
    // Instant.now(),
    // "DOCUMENT",
    // documentId,
    // "S3_UPLOADED",
    // trigger,
    // null,
    // idempotencyKey,
    // metadata);
    // }
}