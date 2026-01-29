package com.skillstorm.finsight.compliance_event.models;

import java.math.BigDecimal;
import java.time.Instant;

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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "compliance_event", schema = "compliance_event", uniqueConstraints = @UniqueConstraint(name = "uk_event_source_entity", columnNames = {
        "source_system", "source_entity_id" }))
public class ComplianceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id", nullable = false, updatable = false)
    private Long eventId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 16)
    private EventType eventType;

    @NotBlank
    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem;

    @NotBlank
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

    @NotNull
    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32)
    private EventStatus status;

    @Min(0)
    @Max(100)
    @Column(name = "severity_score")
    private Integer severityScore;

    @Column(name = "correlation_id", length = 128)
    private String correlationId;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    public ComplianceEvent() {
    }

    /*
     * --------------------
     * Lifecycle Guardrails
     * --------------------
     */

    @PrePersist
    void prePersist() {
        if (eventTime == null) {
            eventTime = Instant.now();
        }
        applyDefaultStatus();
        validateStatusForType();
        validateSeverityForType();
    }

    @PreUpdate
    void preUpdate() {
        validateStatusForType();
        validateSeverityForType();
    }

    private void applyDefaultStatus() {
        if (status != null || eventType == null) {
            return;
        }

        switch (eventType) {
            case CTR -> status = EventStatus.CREATED;
            case SAR -> status = EventStatus.DRAFT;
        }
    }

    private void validateStatusForType() {
        if (eventType == null || status == null) {
            return;
        }

        switch (eventType) {
            case CTR -> {
                if (status != EventStatus.CREATED &&
                        status != EventStatus.FILED) {
                    throw new IllegalStateException(
                            "CTR status must be CREATED or FILED");
                }
            }
            case SAR -> {
                if (status != EventStatus.DRAFT &&
                        status != EventStatus.SUBMITTED) {
                    throw new IllegalStateException(
                            "SAR status must be DRAFT or SUBMITTED");
                }
            }
        }
    }

    private void validateSeverityForType() {
        if (severityScore == null || eventType == null) {
            return;
        }

        if (eventType == EventType.CTR) {
            throw new IllegalStateException(
                    "CTR events cannot have severityScore");
        }

        if (severityScore < 0 || severityScore > 100) {
            throw new IllegalStateException(
                    "SAR severityScore must be between 0 and 100");
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

    public void setSuspectSnapshot(
            SuspectSnapshotAtTimeOfEvent suspectSnapshot) {
        this.suspectSnapshot = suspectSnapshot;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public void setEventTime(Instant eventTime) {
        this.eventTime = eventTime;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
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

    public Instant getCreatedAt() {
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
