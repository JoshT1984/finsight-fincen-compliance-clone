package com.skillstorm.finsight.compliance_event.models;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "compliance_event", schema = "compliance_event", uniqueConstraints = @UniqueConstraint(name = "uk_source_system_entity", columnNames = {
        "source_system", "source_entity_id" }))
public class ComplianceEvent {

    public enum ComplianceEventStatus {
        CREATED,
        FILED,
        DRAFT,
        SUBMITTED
    }

    public enum EventType {
        CTR,
        SAR
    }

    public enum SubjectType {
        ACCOUNT,
        CUSTOMER,
        SUSPECT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id", updatable = false, nullable = false)
    private Long eventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 16)
    private EventType eventType;

    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem;

    @Column(name = "source_entity_id", nullable = false, length = 64)
    private String sourceEntityId;

    @Column(name = "external_subject_key", length = 128)
    private String externalSubjectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_subject_type", length = 32)
    private SubjectType sourceSubjectType;

    @Column(name = "source_subject_id", length = 128)
    private String sourceSubjectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suspect_snapshot_id")
    private SuspectSnapshotAtTimeOfEvent suspectSnapshot;

    @Column(name = "event_time", nullable = false)
    private OffsetDateTime eventTime;

    @DecimalMin("0.00")
    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount;

    // Schema allows NULL; DB CHECK validates when present.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32)
    private ComplianceEventStatus status;

    // Schema allows NULL; DB CHECK enforces 0..100 when present.
    @Min(0)
    @Max(100)
    @Column(name = "severity_score")
    private Integer severityScore;

    @Column(name = "correlation_id", length = 128)
    private String correlationId;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    // DB-managed default now(); keep as read-only from JPA.
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    protected ComplianceEvent() {
    }

    /*
     * --------------------
     * Lifecycle Guardrails
     * --------------------
     */

    @PrePersist
    void prePersist() {
        // event_time is NOT NULL in DB; provide a sensible default.
        if (eventTime == null) {
            eventTime = OffsetDateTime.now();
        }
        applyDefaultStatus();
        validateStatusForType();
    }

    @PreUpdate
    void preUpdate() {
        validateStatusForType();
    }

    private void applyDefaultStatus() {
        if (status != null || eventType == null)
            return;

        switch (eventType) {
            case CTR -> status = ComplianceEventStatus.CREATED;
            case SAR -> status = ComplianceEventStatus.DRAFT;
        }
    }

    private void validateStatusForType() {
        if (eventType == null || status == null)
            return;

        switch (eventType) {
            case CTR -> {
                if (status != ComplianceEventStatus.CREATED && status != ComplianceEventStatus.FILED) {
                    throw new IllegalStateException("CTR status must be CREATED or FILED");
                }
            }
            case SAR -> {
                if (status != ComplianceEventStatus.DRAFT && status != ComplianceEventStatus.SUBMITTED) {
                    throw new IllegalStateException("SAR status must be DRAFT or SUBMITTED");
                }
            }
        }
    }

    /*
     * --------------------
     * Getters / Setters
     * --------------------
     */

    public Long getEventId() {
        return eventId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getSourceEntityId() {
        return sourceEntityId;
    }

    public void setSourceEntityId(String sourceEntityId) {
        this.sourceEntityId = sourceEntityId;
    }

    public String getExternalSubjectKey() {
        return externalSubjectKey;
    }

    public void setExternalSubjectKey(String externalSubjectKey) {
        this.externalSubjectKey = externalSubjectKey;
    }

    public SubjectType getSourceSubjectType() {
        return sourceSubjectType;
    }

    public void setSourceSubjectType(SubjectType sourceSubjectType) {
        this.sourceSubjectType = sourceSubjectType;
    }

    public String getSourceSubjectId() {
        return sourceSubjectId;
    }

    public void setSourceSubjectId(String sourceSubjectId) {
        this.sourceSubjectId = sourceSubjectId;
    }

    public SuspectSnapshotAtTimeOfEvent getSuspectSnapshot() {
        return suspectSnapshot;
    }

    public void setSuspectSnapshot(SuspectSnapshotAtTimeOfEvent suspectSnapshot) {
        this.suspectSnapshot = suspectSnapshot;
    }

    public OffsetDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(OffsetDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public ComplianceEventStatus getStatus() {
        return status;
    }

    public void setStatus(ComplianceEventStatus status) {
        this.status = status;
    }

    public Integer getSeverityScore() {
        return severityScore;
    }

    public void setSeverityScore(Integer severityScore) {
        this.severityScore = severityScore;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    /*
     * --------------------
     * equals / hashCode
     * --------------------
     */

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ComplianceEvent))
            return false;
        ComplianceEvent that = (ComplianceEvent) o;
        return eventId != null && eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ComplianceEvent{" +
                "eventId=" + eventId +
                ", eventType=" + eventType +
                ", status=" + status +
                ", sourceSystem='" + sourceSystem + '\'' +
                ", sourceEntityId='" + sourceEntityId + '\'' +
                ", eventTime=" + eventTime +
                '}';
    }
}
