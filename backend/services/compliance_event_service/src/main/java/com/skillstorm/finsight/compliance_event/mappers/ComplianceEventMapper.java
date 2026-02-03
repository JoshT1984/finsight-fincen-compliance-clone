package com.skillstorm.finsight.compliance_event.mappers;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.skillstorm.finsight.compliance_event.dtos.ComplianceEventResponse;
import com.skillstorm.finsight.compliance_event.models.ComplianceEvent;
import com.skillstorm.finsight.compliance_event.models.SuspectSnapshotAtTimeOfEvent;

@Component
public class ComplianceEventMapper {

    public ComplianceEventResponse toResponse(ComplianceEvent e) {
        if (e == null) {
            return null;
        }
        Long suspectId = null;
        Map<String, Object> suspectMinimal = null;
        SuspectSnapshotAtTimeOfEvent snapshot = e.getSuspectSnapshot();
        if (snapshot != null) {
            suspectId = snapshot.getSuspectId();
            suspectMinimal = snapshot.getSuspectMinimal() != null ? snapshot.getSuspectMinimal() : Map.of();
        }
        return new ComplianceEventResponse(
                e.getEventId(),
                e.getEventType(),
                e.getSourceSystem(),
                e.getSourceEntityId(),
                e.getEventTime(),
                e.getTotalAmount(),
                e.getStatus(),
                e.getSeverityScore(),
                e.getCreatedAt(),
                suspectId,
                suspectMinimal);
    }
}
