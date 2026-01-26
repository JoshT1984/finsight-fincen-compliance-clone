package com.skillstorm.finsight.documents_cases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillstorm.finsight.documents_cases.models.AuditEvent;
import com.skillstorm.finsight.documents_cases.repositories.AuditEventRepository;
import com.skillstorm.finsight.documents_cases.utils.SecurityContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;

/**
 * Service for creating and managing audit events.
 * 
 * <p>This service tracks all operations (CRUD and business actions) across
 * the documents-cases-service, storing field-level changes in metadata.
 */
@Service
public class AuditEventService {
    
    private static final Logger log = LoggerFactory.getLogger(AuditEventService.class);
    
    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;
    
    public AuditEventService(AuditEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Creates an audit event with the specified parameters.
     * 
     * @param action The action performed (e.g., "CREATE", "UPDATE", "DELETE", "REFER", "CLOSE", "UPLOAD")
     * @param entityType The type of entity (e.g., "DOCUMENT", "CASE", "CASE_NOTE")
     * @param entityId The ID of the entity (as String to support different ID types)
     * @param metadata Additional metadata to store (will be serialized to JSON)
     * @return The created audit event
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditEvent createAuditEvent(String action, String entityType, String entityId, Map<String, Object> metadata) {
        try {
            UUID actorUserId = SecurityContextUtils.getCurrentUserId().orElse(null);
            
            String metadataJson = null;
            if (metadata != null && !metadata.isEmpty()) {
                try {
                    metadataJson = objectMapper.writeValueAsString(metadata);
                } catch (JsonProcessingException e) {
                    log.error("Failed to serialize metadata to JSON for audit event", e);
                    metadataJson = "{\"error\": \"Failed to serialize metadata\"}";
                }
            }
            
            AuditEvent auditEvent = new AuditEvent();
            auditEvent.setActorUserId(actorUserId);
            auditEvent.setAction(action);
            auditEvent.setEntityType(entityType);
            auditEvent.setEntityId(entityId);
            auditEvent.setMetadata(metadataJson);
            auditEvent.setCreatedAt(Instant.now());
            
            AuditEvent saved = repository.save(auditEvent);
            log.debug("Created audit event: {} for {} {} (action: {})", 
                    saved.getAuditId(), entityType, entityId, action);
            
            return saved;
        } catch (Exception e) {
            // Don't fail the main operation if audit event creation fails
            log.error("Failed to create audit event for {} {} (action: {})", 
                    entityType, entityId, action, e);
            return null;
        }
    }
    
    /**
     * Creates an audit event for a CREATE operation.
     * 
     * @param entityType The type of entity
     * @param entityId The ID of the entity
     * @param entity The created entity (for metadata)
     * @return The created audit event
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditEvent auditCreate(String entityType, String entityId, Object entity) {
        Map<String, Object> metadata = new HashMap<>();
        if (entity != null) {
            metadata.put("entity", serializeEntity(entity));
        }
        return createAuditEvent("CREATE", entityType, entityId, metadata);
    }
    
    /**
     * Creates an audit event for an UPDATE operation, comparing old and new entities.
     * 
     * @param entityType The type of entity
     * @param entityId The ID of the entity
     * @param oldEntity The entity before the update
     * @param newEntity The entity after the update
     * @return The created audit event
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditEvent auditUpdate(String entityType, String entityId, Object oldEntity, Object newEntity) {
        Map<String, Object> metadata = new HashMap<>();
        
        if (oldEntity != null && newEntity != null) {
            Map<String, Map<String, Object>> changes = compareEntities(oldEntity, newEntity);
            // Always include changes if there are any
            if (!changes.isEmpty()) {
                metadata.put("changes", changes);
            }
        }
        
        // Don't include full entity in UPDATE events - changes are more important
        // Only include entity if there are no changes (edge case)
        if (metadata.get("changes") == null && newEntity != null) {
            metadata.put("entity", serializeEntity(newEntity));
        }
        
        return createAuditEvent("UPDATE", entityType, entityId, metadata);
    }
    
    /**
     * Creates an audit event for a DELETE operation.
     * 
     * @param entityType The type of entity
     * @param entityId The ID of the entity
     * @param entity The deleted entity (for metadata)
     * @return The created audit event
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditEvent auditDelete(String entityType, String entityId, Object entity) {
        Map<String, Object> metadata = new HashMap<>();
        if (entity != null) {
            metadata.put("entity", serializeEntity(entity));
        }
        return createAuditEvent("DELETE", entityType, entityId, metadata);
    }
    
    /**
     * Creates an audit event for a business action (e.g., REFER, CLOSE, UPLOAD).
     * 
     * @param entityType The type of entity
     * @param entityId The ID of the entity
     * @param action The action name
     * @param metadata Additional metadata for the action
     * @return The created audit event
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditEvent auditAction(String entityType, String entityId, String action, Map<String, Object> metadata) {
        return createAuditEvent(action, entityType, entityId, metadata);
    }
    
    /**
     * Compares two entities and returns a map of changed fields.
     * 
     * @param oldEntity The entity before the change
     * @param newEntity The entity after the change
     * @return Map of field names to maps containing "old" and "new" values
     */
    private Map<String, Map<String, Object>> compareEntities(Object oldEntity, Object newEntity) {
        Map<String, Map<String, Object>> changes = new HashMap<>();
        
        if (oldEntity.getClass() != newEntity.getClass()) {
            log.warn("Cannot compare entities of different types: {} vs {}", 
                    oldEntity.getClass(), newEntity.getClass());
            return changes;
        }
        
        Class<?> clazz = oldEntity.getClass();
        Field[] fields = clazz.getDeclaredFields();
        
        for (Field field : fields) {
            // Skip synthetic fields and the primary key field only
            if (field.isSynthetic() || field.getAnnotation(jakarta.persistence.Id.class) != null) {
                continue;
            }
            
            try {
                field.setAccessible(true);
                Object oldValue = field.get(oldEntity);
                Object newValue = field.get(newEntity);
                
                // Convert enum values to strings for better serialization
                if (oldValue != null && oldValue.getClass().isEnum()) {
                    oldValue = oldValue.toString();
                }
                if (newValue != null && newValue.getClass().isEnum()) {
                    newValue = newValue.toString();
                }
                
                // Compare values (handling nulls)
                if (!Objects.equals(oldValue, newValue)) {
                    Map<String, Object> change = new HashMap<>();
                    change.put("old", oldValue);
                    change.put("new", newValue);
                    changes.put(field.getName(), change);
                }
            } catch (IllegalAccessException e) {
                log.debug("Could not access field {} for comparison", field.getName());
            }
        }
        
        return changes;
    }
    
    /**
     * Serializes an entity to a map for storage in metadata.
     * Only includes non-null, non-ID fields to avoid circular references.
     * 
     * @param entity The entity to serialize
     * @return Map representation of the entity
     */
    private Map<String, Object> serializeEntity(Object entity) {
        Map<String, Object> result = new HashMap<>();
        
        if (entity == null) {
            return result;
        }
        
        Class<?> clazz = entity.getClass();
        Field[] fields = clazz.getDeclaredFields();
        
        for (Field field : fields) {
            if (field.isSynthetic()) {
                continue;
            }
            
            try {
                field.setAccessible(true);
                Object value = field.get(entity);
                
                // Only include non-null values, and skip complex objects to avoid circular references
                if (value != null && isSimpleType(value.getClass())) {
                    result.put(field.getName(), value);
                }
            } catch (IllegalAccessException e) {
                log.debug("Could not access field {} for serialization", field.getName());
            }
        }
        
        return result;
    }
    
    /**
     * Checks if a type is a simple type that can be safely serialized.
     */
    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
               clazz == String.class ||
               clazz == Integer.class ||
               clazz == Long.class ||
               clazz == Double.class ||
               clazz == Float.class ||
               clazz == Boolean.class ||
               clazz == Byte.class ||
               clazz == Short.class ||
               clazz == Character.class ||
               clazz == java.time.Instant.class ||
               clazz == java.util.UUID.class ||
               Number.class.isAssignableFrom(clazz);
    }
}
