package com.skillstorm.finsight.compliance_event.models;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.domain.Persistable;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.micrometer.common.lang.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "compliance_event_ctr_detail", schema = "compliance_event")
public class ComplianceEventCtrDetail implements Persistable<Long> {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private Long eventId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "event_id", nullable = false)
    @JsonIgnore
    private ComplianceEvent event;

    @NotNull
    @Column(name = "customer_name", nullable = false, length = 128)
    private String customerName;

    @NotNull
    @Column(name = "transaction_time", nullable = false)
    private Instant transactionTime;

    @NotNull
    @Convert(converter = JsonMapConverter.class)
    @Column(name = "ctr_form_data", nullable = false, columnDefinition = "json")
    private Map<String, Object> ctrFormData;

    // present in table, but DB-managed default; no need to set manually
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    // present in table, default 'CTR'
    @Column(name = "event_type", insertable = false, updatable = false, length = 16)
    private String eventType;

    // ---- Persistable support (forces INSERT vs MERGE for shared PK entities)
    @Transient
    private boolean isNew = true;

    @Override
    @Nullable
    public Long getId() {
        return eventId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    void markNotNewOnLoad() {
        this.isNew = false;
    }

    @PostPersist
    void markNotNewOnPersist() {
        this.isNew = false;
    }

    // ---- getters / setters

    public Long getEventId() {
        return eventId;
    }

    public ComplianceEvent getEvent() {
        return event;
    }

    public void setEvent(ComplianceEvent event) {
        this.event = event;
        if (event != null) {
            this.eventId = event.getEventId();
        }
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

    public String getEventType() {
        return eventType;
    }
}
