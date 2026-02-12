package com.skillstorm.finsight.compliance_event.models;

import java.time.Instant;
import java.util.HashMap;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "compliance_event_ctr_detail", schema = "compliance_event")
public class ComplianceEventCtrDetail {

    @Id
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    /**
     * Discriminator used by the schema to enforce CTR-only attachment via
     * composite FK (event_id, event_type). DB default is 'CTR'.
     * Marked insertable=false so the database default is used.
     */
 
    @Column(name = "event_type", nullable = false, length = 16, insertable = false, updatable = false)
    private String eventType;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "event_id", nullable = false, updatable = false)
    private ComplianceEvent event;

    @NotBlank
    @Column(name = "customer_name", nullable = false, length = 128)
    private String customerName;

    @NotNull
    @Column(name = "transaction_time", nullable = false)
    private Instant transactionTime;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ctr_form_data", columnDefinition = "json", nullable = false)
    private Map<String, Object> ctrFormData;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private Instant createdAt;

    public ComplianceEventCtrDetail() {
    }

    @PrePersist
    void prePersist() {
        if (ctrFormData == null) {
            ctrFormData = new HashMap<>();
        }
    }

    public Long getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    /** Set before persist so @NotNull validation passes; column is insertable=false so DB default is used on INSERT. */
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public ComplianceEvent getEvent() {
        return event;
    }

    public void setEvent(ComplianceEvent event) {
        this.event = event;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Instant getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(Instant transactionTime) {
        this.transactionTime = transactionTime;
    }

    public Map<String, Object> getCtrFormData() {
        return ctrFormData;
    }

    public void setCtrFormData(Map<String, Object> ctrFormData) {
        this.ctrFormData = ctrFormData;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ComplianceEventCtrDetail))
            return false;
        ComplianceEventCtrDetail that = (ComplianceEventCtrDetail) o;
        return eventId != null && eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ComplianceEventCtrDetail{" +
                "eventId=" + eventId +
                ", eventType=" + eventType +
                ", transactionTime=" + transactionTime +
                ", createdAt=" + createdAt +
                '}';
    }
}
