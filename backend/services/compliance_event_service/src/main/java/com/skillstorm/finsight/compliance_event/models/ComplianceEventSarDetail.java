package com.skillstorm.finsight.compliance_event.models;

import java.time.OffsetDateTime;
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

@Entity
@Table(name = "compliance_event_sar_detail", schema = "compliance_event")
public class ComplianceEventSarDetail {

    @Id
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "event_id", nullable = false, updatable = false)
    private ComplianceEvent event;

    @Column(name = "narrative", columnDefinition = "text")
    private String narrative;

    @Column(name = "activity_start")
    private OffsetDateTime activityStart;

    @Column(name = "activity_end")
    private OffsetDateTime activityEnd;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "form_data", columnDefinition = "json", nullable = false)
    private Map<String, Object> formData;

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    // DB-managed DEFAULT now()
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    protected ComplianceEventSarDetail() {
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

    public OffsetDateTime getActivityStart() {
        return activityStart;
    }

    public void setActivityStart(OffsetDateTime activityStart) {
        this.activityStart = activityStart;
    }

    public OffsetDateTime getActivityEnd() {
        return activityEnd;
    }

    public void setActivityEnd(OffsetDateTime activityEnd) {
        this.activityEnd = activityEnd;
    }

    public Map<String, Object> getFormData() {
        return formData;
    }

    public void setFormData(Map<String, Object> formData) {
        this.formData = formData;
    }

    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(OffsetDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public OffsetDateTime getCreatedAt() {
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
                ", submittedAt=" + submittedAt +
                ", createdAt=" + createdAt +
                '}';
    }
}
