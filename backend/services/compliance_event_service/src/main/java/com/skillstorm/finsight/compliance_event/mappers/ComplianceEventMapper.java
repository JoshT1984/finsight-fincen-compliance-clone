package com.skillstorm.finsight.compliance_event.mappers;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.skillstorm.finsight.compliance_event.dtos.ComplianceEventResponse;
import com.skillstorm.finsight.compliance_event.models.ComplianceEvent;
import com.skillstorm.finsight.compliance_event.models.ComplianceEventCtrDetail;
import com.skillstorm.finsight.compliance_event.models.ComplianceEventSarDetail;
import com.skillstorm.finsight.compliance_event.models.EventType;
import com.skillstorm.finsight.compliance_event.models.SuspectSnapshotAtTimeOfEvent;
import com.skillstorm.finsight.compliance_event.repositories.ComplianceEventCtrDetailRepository;
import com.skillstorm.finsight.compliance_event.repositories.ComplianceEventSarDetailRepository;

@Component
public class ComplianceEventMapper {

    private final ComplianceEventCtrDetailRepository ctrDetailRepository;
    private final ComplianceEventSarDetailRepository sarDetailRepository;

    public ComplianceEventMapper(
            ComplianceEventCtrDetailRepository ctrDetailRepository,
            ComplianceEventSarDetailRepository sarDetailRepository) {
        this.ctrDetailRepository = ctrDetailRepository;
        this.sarDetailRepository = sarDetailRepository;
    }

    public ComplianceEventResponse toResponse(ComplianceEvent e) {
        if (e == null) {
            return null;
        }

        Long suspectId = null;
        Map<String, Object> suspectMinimal = null;

        SuspectSnapshotAtTimeOfEvent snapshot = e.getSuspectSnapshot();
        if (snapshot != null) {
            suspectId = snapshot.getSuspectId();
            suspectMinimal = snapshot.getSuspectMinimal() != null
                    ? snapshot.getSuspectMinimal()
                    : Map.of();
        }

        String customerName = null;

        // Prefer enum stored on ComplianceEvent if present
        String subjectType = e.getSourceSubjectType() != null ? e.getSourceSubjectType().name() : null;

        if (e.getEventType() == EventType.CTR) {
            Optional<ComplianceEventCtrDetail> ctrDetailOpt = ctrDetailRepository.findByEventId(e.getEventId());
            if (ctrDetailOpt.isPresent()) {
                ComplianceEventCtrDetail ctrDetail = ctrDetailOpt.get();
                customerName = ctrDetail.getCustomerName();

                // fallback: try to find subject type from ctr form data if
                // event.sourceSubjectType is null
                if (subjectType == null) {
                    subjectType = pickString(ctrDetail.getCtrFormData(),
                            "subjectType", "sourceSubjectType", "source_subject_type");
                }
            }
        } else if (e.getEventType() == EventType.SAR) {
            Optional<ComplianceEventSarDetail> sarDetailOpt = sarDetailRepository.findByEventId(e.getEventId());
            if (sarDetailOpt.isPresent()) {
                ComplianceEventSarDetail sarDetail = sarDetailOpt.get();

                // SAR detail has only formData, so we infer from that
                customerName = pickString(sarDetail.getFormData(),
                        "customerName", "subjectName", "subject_name");

                if (subjectType == null) {
                    subjectType = pickString(sarDetail.getFormData(),
                            "subjectType", "sourceSubjectType", "source_subject_type");
                }
            }
        }

        return new ComplianceEventResponse(
                e.getEventId(),
                e.getEventType(),
                e.getSourceSystem(),
                e.getSourceEntityId(),
                customerName,
                subjectType,
                e.getEventTime(),
                e.getTotalAmount(),
                e.getStatus(),
                e.getSeverityScore(),
                e.getCreatedAt(),
                suspectId,
                suspectMinimal);
    }

    private String pickString(Map<String, Object> map, String... keys) {
        if (map == null || map.isEmpty() || keys == null)
            return null;

        for (String k : keys) {
            Object v = map.get(k);
            if (v == null)
                continue;
            String s = String.valueOf(v).trim();
            if (!s.isEmpty())
                return s;
        }
        return null;
    }
}
