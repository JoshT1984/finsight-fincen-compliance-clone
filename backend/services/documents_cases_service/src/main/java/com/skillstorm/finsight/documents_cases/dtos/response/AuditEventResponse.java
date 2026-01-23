package com.skillstorm.finsight.documents_cases.dtos.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillstorm.finsight.documents_cases.models.AuditEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for audit events.
 * 
 * <p>Converts AuditEvent entities to response format, parsing JSONB metadata
 * into a Map for easier consumption by clients.
 */
public record AuditEventResponse(
        Long auditId,
        UUID actorUserId,
        String action,
        String entityType,
        String entityId,
        Map<String, Object> metadata,
        Instant createdAt
) {
    
    /**
     * Converts an AuditEvent entity to an AuditEventResponse.
     * 
     * @param auditEvent The audit event entity
     * @param objectMapper ObjectMapper for parsing JSON metadata
     * @return AuditEventResponse
     */
    public static AuditEventResponse fromEntity(AuditEvent auditEvent, ObjectMapper objectMapper) {
        Map<String, Object> metadataMap = parseMetadata(auditEvent.getMetadata(), objectMapper);
        
        return new AuditEventResponse(
                auditEvent.getAuditId(),
                auditEvent.getActorUserId(),
                auditEvent.getAction(),
                auditEvent.getEntityType(),
                auditEvent.getEntityId(),
                metadataMap,
                auditEvent.getCreatedAt()
        );
    }
    
    /**
     * Parses JSONB metadata string into a Map.
     * 
     * @param metadataJson The JSON string from the database
     * @param objectMapper ObjectMapper for parsing
     * @return Map representation of metadata, or empty map if parsing fails
     */
    private static Map<String, Object> parseMetadata(String metadataJson, ObjectMapper objectMapper) {
        if (metadataJson == null || metadataJson.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.readValue(metadataJson, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            // If parsing fails, return empty map rather than throwing exception
            return new HashMap<>();
        }
    }
}
