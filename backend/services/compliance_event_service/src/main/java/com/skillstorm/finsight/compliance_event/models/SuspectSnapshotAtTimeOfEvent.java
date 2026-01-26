package com.skillstorm.finsight.compliance_event.models;

import java.time.OffsetDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "suspect_snapshot_at_time_of_event", schema = "compliance_event")
public class SuspectSnapshotAtTimeOfEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "snapshot_id", nullable = false, updatable = false)
    private Long snapshotId;

    @Column(name = "suspect_id", nullable = false)
    private Long suspectId;

    @Column(name = "last_known_alias", length = 256)
    private String lastKnownAlias;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "last_known_address", columnDefinition = "json")
    private Map<String, Object> lastKnownAddress;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "suspect_minimal", columnDefinition = "json")
    private Map<String, Object> suspectMinimal;

    // DB-managed DEFAULT now()
    @Column(name = "captured_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime capturedAt;

    protected SuspectSnapshotAtTimeOfEvent() {
    }

    @PrePersist
    void prePersist() {
        if (lastKnownAddress == null) {
            lastKnownAddress = Map.of();
        }
        if (suspectMinimal == null) {
            suspectMinimal = Map.of();
        }
    }

    public Long getSnapshotId() {
        return snapshotId;
    }

    public Long getSuspectId() {
        return suspectId;
    }

    public void setSuspectId(Long suspectId) {
        this.suspectId = suspectId;
    }

    public String getLastKnownAlias() {
        return lastKnownAlias;
    }

    public void setLastKnownAlias(String lastKnownAlias) {
        this.lastKnownAlias = lastKnownAlias;
    }

    public Map<String, Object> getLastKnownAddress() {
        return lastKnownAddress;
    }

    public void setLastKnownAddress(Map<String, Object> lastKnownAddress) {
        this.lastKnownAddress = lastKnownAddress;
    }

    public Map<String, Object> getSuspectMinimal() {
        return suspectMinimal;
    }

    public void setSuspectMinimal(Map<String, Object> suspectMinimal) {
        this.suspectMinimal = suspectMinimal;
    }

    public OffsetDateTime getCapturedAt() {
        return capturedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SuspectSnapshotAtTimeOfEvent))
            return false;
        SuspectSnapshotAtTimeOfEvent that = (SuspectSnapshotAtTimeOfEvent) o;
        return snapshotId != null && snapshotId.equals(that.snapshotId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "SuspectSnapshotAtTimeOfEvent{" +
                "snapshotId=" + snapshotId +
                ", suspectId=" + suspectId +
                ", capturedAt=" + capturedAt +
                '}';
    }
}
