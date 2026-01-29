package com.skillstorm.finsight.compliance_event.mappers;

import com.skillstorm.finsight.compliance_event.dtos.ComplianceEventResponse;
import com.skillstorm.finsight.compliance_event.models.ComplianceEvent;

import org.springframework.stereotype.Component;

@Component
public class ComplianceEventMapper {

    public ComplianceEventResponse toResponse(ComplianceEvent e) {
        if (e == null) {
            return null;
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
                e.getCreatedAt());
    }
}
