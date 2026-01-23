package com.skillstorm.finsight.documents_cases.repositories;

import com.skillstorm.finsight.documents_cases.models.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    
    /**
     * Finds all audit events for a specific entity.
     * 
     * @param entityType The type of entity (e.g., "DOCUMENT", "CASE", "CASE_NOTE")
     * @param entityId The ID of the entity
     * @return List of audit events for the entity
     */
    List<AuditEvent> findByEntityTypeAndEntityId(String entityType, String entityId);
    
    /**
     * Finds all audit events for a specific entity, ordered by creation date descending (newest first).
     * 
     * @param entityType The type of entity
     * @param entityId The ID of the entity
     * @return List of audit events for the entity, newest first
     */
    List<AuditEvent> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, String entityId);
    
    /**
     * Finds all audit events performed by a specific user.
     * 
     * @param actorUserId The user ID
     * @return List of audit events by the user
     */
    List<AuditEvent> findByActorUserId(UUID actorUserId);
    
    /**
     * Finds all audit events for a specific action.
     * 
     * @param action The action name (e.g., "CREATE", "UPDATE", "DELETE", "REFER", "CLOSE", "UPLOAD")
     * @return List of audit events for the action
     */
    List<AuditEvent> findByAction(String action);
}
