package com.skillstorm.finsight.compliance_event.models;

import java.time.OffsetDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "compliance_event_link", schema = "compliance_event")
public class ComplianceEventLink {

    public enum LinkType {
        SAR_SUPPORTS_CTR,
        CTR_SUPPORTS_SAR,
        RELATED
    }

    @EmbeddedId
    private ComplianceEventLinkId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("fromEventId")
    @JoinColumn(name = "from_event_id", nullable = false, updatable = false)
    private ComplianceEvent fromEvent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("toEventId")
    @JoinColumn(name = "to_event_id", nullable = false, updatable = false)
    private ComplianceEvent toEvent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_snapshot", columnDefinition = "json", nullable = false)
    private Map<String, Object> evidenceSnapshot;

    // DB-managed DEFAULT now()
    @Column(name = "linked_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime linkedAt;

    protected ComplianceEventLink() {
    }

    public ComplianceEventLink(ComplianceEvent fromEvent, ComplianceEvent toEvent, LinkType linkType) {
        this.fromEvent = fromEvent;
        this.toEvent = toEvent;
        this.id = new ComplianceEventLinkId(fromEvent.getEventId(), toEvent.getEventId(), linkType);
    }

    @PrePersist
    void prePersist() {
        // Keep JSON non-null to satisfy NOT NULL + DEFAULT {} semantics consistently
        if (evidenceSnapshot == null) {
            evidenceSnapshot = Map.of();
        }

        // Auto-build EmbeddedId if caller used setters instead of constructor
        if (id == null) {
            if (fromEvent == null || toEvent == null) {
                throw new IllegalStateException("ComplianceEventLink requires fromEvent and toEvent.");
            }
            if (fromEvent.getEventId() == null || toEvent.getEventId() == null) {
                throw new IllegalStateException(
                        "ComplianceEventLink requires persisted ComplianceEvent IDs before linking.");
            }
            throw new IllegalStateException(
                    "ComplianceEventLinkId is missing. Use constructor or setId(new ComplianceEventLinkId(...)).");
        }

        // Guard against accidental self-links (also enforced in DB CHECK)
        if (id.getFromEventId() != null && id.getFromEventId().equals(id.getToEventId())) {
            throw new IllegalStateException("ComplianceEventLink cannot link an event to itself.");
        }
    }

    public ComplianceEventLinkId getId() {
        return id;
    }

    public void setId(ComplianceEventLinkId id) {
        this.id = id;
    }

    public ComplianceEvent getFromEvent() {
        return fromEvent;
    }

    public ComplianceEvent getToEvent() {
        return toEvent;
    }

    public Map<String, Object> getEvidenceSnapshot() {
        return evidenceSnapshot;
    }

    public void setEvidenceSnapshot(Map<String, Object> evidenceSnapshot) {
        this.evidenceSnapshot = evidenceSnapshot;
    }

    public OffsetDateTime getLinkedAt() {
        return linkedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ComplianceEventLink))
            return false;
        ComplianceEventLink that = (ComplianceEventLink) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ComplianceEventLink{" +
                "id="
                + (id != null ? (id.getFromEventId() + "->" + id.getToEventId() + ":" + id.getLinkType()) : "null") +
                ", linkedAt=" + linkedAt +
                '}';
    }
}
