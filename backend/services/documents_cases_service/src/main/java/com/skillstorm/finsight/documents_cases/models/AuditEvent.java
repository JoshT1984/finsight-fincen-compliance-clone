package com.skillstorm.finsight.documents_cases.models;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 64)
    private String entityId;

    @Column(name = "metadata", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AuditEvent() {
    }

    public AuditEvent(Long auditId, UUID actorUserId, String action, String entityType, String entityId, String metadata, Instant createdAt) {
        this.auditId = auditId;
        this.actorUserId = actorUserId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }
    public Long getAuditId() {
        return auditId;
    }

    public void setAuditId(Long auditId) {
        this.auditId = auditId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(UUID actorUserId) {
        this.actorUserId = actorUserId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditEvent that = (AuditEvent) o;
        return Objects.equals(auditId, that.auditId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(auditId);
    }

    @Override
    public String toString() {
        return "AuditEvent{" +
                "auditId=" + auditId +
                ", actorUserId=" + actorUserId +
                ", action='" + action + '\'' +
                ", entityType='" + entityType + '\'' +
                ", entityId='" + entityId + '\'' +
                ", metadata='" + metadata + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
