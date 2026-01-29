package com.skillstorm.finsight.compliance_event.models;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;

@Embeddable
public class ComplianceEventLinkId implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Column(name = "from_event_id", nullable = false)
    private Long fromEventId;

    @NotNull
    @Column(name = "to_event_id", nullable = false)
    private Long toEventId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 32)
    private ComplianceEventLink.LinkType linkType;

    protected ComplianceEventLinkId() {
    }

    public ComplianceEventLinkId(
            Long fromEventId,
            Long toEventId,
            ComplianceEventLink.LinkType linkType) {

        this.fromEventId = fromEventId;
        this.toEventId = toEventId;
        this.linkType = linkType;
    }

    public Long getFromEventId() {
        return fromEventId;
    }

    public Long getToEventId() {
        return toEventId;
    }

    public ComplianceEventLink.LinkType getLinkType() {
        return linkType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ComplianceEventLinkId))
            return false;
        ComplianceEventLinkId that = (ComplianceEventLinkId) o;
        return Objects.equals(fromEventId, that.fromEventId)
                && Objects.equals(toEventId, that.toEventId)
                && linkType == that.linkType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromEventId, toEventId, linkType);
    }
}
