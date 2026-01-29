package com.skillstorm.finsight.compliance_event.models;

import java.time.Instant;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "compliance_event_sar_detail", schema = "compliance_event")
public class ComplianceEventSarDetail {

    @Id
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    /**
     * Discriminator used by the schema to enforce SAR-only attachment via
     * composite FK (event_id, event_type). DB default is 'SAR'.
     * Marked insertable=false so the database default is used.
     */
    @NotNull
    @Column(name = "event_type", nullable = false, length = 16, insertable = false, updatable = false)
    private String eventType;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "event_id", nullable = false, updatable = false)
    private ComplianceEvent event;

    @Column(name = "narrative", columnDefinition = "text")
    private String narrative;

    @Column(name = "activity_start")
    private Instant activityStart;

    @Column(name = "activity_end")
    private Instant activityEnd;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "form_data", columnDefinition = "json", nullable = false)
    private Map<String, Object> formData;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    public ComplianceEventSarDetail() {
    }

    @PrePersist
    void prePersist() {
        if (formData == null) {
            formData = Map.of();
        }
    }

    public Long getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public ComplianceEvent getEvent() {
        return event;
    }

    public void setEvent(ComplianceEvent event) {
        this.event = event;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }

    public Instant getActivityStart() {
        return activityStart;
    }

    public void setActivityStart(Instant activityStart) {
        this.activityStart = activityStart;
    }

    public Instant getActivityEnd() {
        return activityEnd;
    }

    public void setActivityEnd(Instant activityEnd) {
        this.activityEnd = activityEnd;
    }

    public Map<String, Object> getFormData() {
        return formData;
    }

    public void setFormData(Map<String, Object> formData) {
        this.formData = formData;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ComplianceEventSarDetail))
            return false;
        ComplianceEventSarDetail that = (ComplianceEventSarDetail) o;
        return eventId != null && eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ComplianceEventSarDetail{" +
                "eventId=" + eventId +
                ", eventType=" + eventType +
                ", submittedAt=" + submittedAt +
                ", createdAt=" + createdAt +
                '}';
    }
}
