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

    // Helper class for event types
    public static DocumentEventLog documentUploaded(String documentId, String trigger, Map<String, Object> metadata) {
        return of("DOCUMENT", documentId, "UPLOAD", trigger, null, UUID.randomUUID().toString(), metadata);
    }

    public static DocumentEventLog caseFileCreated(String caseFileId, String trigger, Map<String, Object> metadata) {
        return of("CASE_FILE", caseFileId, "CREATED", trigger, null, UUID.randomUUID().toString(), metadata);
    }

    public static DocumentEventLog caseFileReferred(String caseFileId, String trigger, Map<String, Object> metadata) {
        return of("CASE_FILE", caseFileId, "REFERRED", trigger, null, UUID.randomUUID().toString(), metadata);
    }

    public static class CaseFileEvents {
        public static final String CREATED = "CREATED";
        public static final String REFERRED = "REFERRED";
        public static final String UPDATED = "UPDATED";
        public static final String CLOSED = "CLOSED";
        public static final String DELETED = "DELETED";
    }

    public static DocumentEventLog caseFileUpdated(String caseFileId, String trigger, Map<String, Object> metadata) {
        return of("CASE_FILE", caseFileId, CaseFileEvents.UPDATED, trigger, null, UUID.randomUUID().toString(),
                metadata);
    }

    public static DocumentEventLog caseFileClosed(String caseFileId, String trigger, Map<String, Object> metadata) {
        return of("CASE_FILE", caseFileId, CaseFileEvents.CLOSED, trigger, null, UUID.randomUUID().toString(),
                metadata);
    }

    public static DocumentEventLog caseFileDeleted(String caseFileId, String trigger, Map<String, Object> metadata) {
        return of("CASE_FILE", caseFileId, CaseFileEvents.DELETED, trigger, null, UUID.randomUUID().toString(),
                metadata);
    }

    public static DocumentEventLog caseNoteCreated(String noteId, String trigger, Map<String, Object> metadata) {
        return of("CASE_NOTE", noteId, "CREATED", trigger, null, UUID.randomUUID().toString(), metadata);
    }

    public static DocumentEventLog caseNoteUpdated(String noteId, String trigger, Map<String, Object> metadata) {
        return of("CASE_NOTE", noteId, "UPDATED", trigger, null, UUID.randomUUID().toString(), metadata);
    }

    public static DocumentEventLog caseNoteDeleted(String noteId, String trigger, Map<String, Object> metadata) {
        return of("CASE_NOTE", noteId, "DELETED", trigger, null, UUID.randomUUID().toString(), metadata);
    }

    public static DocumentEventLog ctrRequest(String ctrId, String trigger, Map<String, Object> metadata) {
        return of("CTR", ctrId, "REQUEST", trigger, null, UUID.randomUUID().toString(), metadata);
    }

    public static DocumentEventLog sarRequest(String sarId, String trigger, Map<String, Object> metadata) {
        return of("SAR", sarId, "REQUEST", trigger, null, UUID.randomUUID().toString(), metadata);
    }

    public static DocumentEventLog documentCreated(String documentId, String trigger, Map<String, Object> metadata) {
        return of("DOCUMENT", documentId, "CREATED", trigger, null, UUID.randomUUID().toString(), metadata);
    }

    public static DocumentEventLog documentUpdated(String documentId, String trigger, Map<String, Object> metadata) {
        return of("DOCUMENT", documentId, "UPDATED", trigger, null, UUID.randomUUID().toString(), metadata);
    }

    public static DocumentEventLog documentDeleted(String documentId, String trigger, Map<String, Object> metadata) {
        return of("DOCUMENT", documentId, "DELETED", trigger, null, UUID.randomUUID().toString(), metadata);
    }

    public static DocumentEventLog documentCtrUploaded(String documentId, String trigger,
            Map<String, Object> metadata) {
        return of("DOCUMENT", documentId, "CTR_UPLOADED", trigger, null, UUID.randomUUID().toString(), metadata);
    }

    public static DocumentEventLog documentSarUploaded(String documentId, String trigger,
            Map<String, Object> metadata) {
        return of("DOCUMENT", documentId, "SAR_UPLOADED", trigger, null, UUID.randomUUID().toString(), metadata);
    }

    public static DocumentEventLog documentCaseUploaded(String documentId, String trigger,
            Map<String, Object> metadata) {
        return of("DOCUMENT", documentId, "CASE_UPLOADED", trigger, null, UUID.randomUUID().toString(), metadata);
    }

    public static DocumentEventLog s3Upload(String s3Key, String trigger, Map<String, Object> metadata) {
        return of("S3", s3Key, "UPLOAD", trigger, null, UUID.randomUUID().toString(), metadata);
    }

    public static DocumentEventLog s3UploadFailed(String s3Key, String trigger, Map<String, Object> metadata) {
        return of("S3", s3Key, "UPLOAD_FAILED", trigger, null, UUID.randomUUID().toString(), metadata);
    }

    public static DocumentEventLog s3Delete(String s3Key, String trigger, Map<String, Object> metadata) {
        return of("S3", s3Key, "DELETE", trigger, null, UUID.randomUUID().toString(), metadata);
    }

    public static DocumentEventLog s3Copy(String s3Key, String trigger, Map<String, Object> metadata) {
        return of("S3", s3Key, "COPY", trigger, null, UUID.randomUUID().toString(), metadata);
    }
}