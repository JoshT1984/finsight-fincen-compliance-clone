package com.skillstorm.finsight.compliance_event.mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skillstorm.finsight.compliance_event.models.ComplianceEvent;
import com.skillstorm.finsight.compliance_event.models.ComplianceEventCtrDetail;
import com.skillstorm.finsight.compliance_event.models.EventStatus;
import com.skillstorm.finsight.compliance_event.models.EventType;
import com.skillstorm.finsight.compliance_event.models.SubjectType;
import com.skillstorm.finsight.compliance_event.repositories.CtrAggregationRow;

@Component
public class CtrGenerationMapper {

    private final ObjectMapper objectMapper;

    public CtrGenerationMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ComplianceEvent toCtrEvent(
            String subjectKey,
            String sourceSubjectType,
            LocalDate day,
            BigDecimal totalAmount,
            Integer suspicionScore,
            String idempotencyKey) {

        ComplianceEvent e = new ComplianceEvent();
        e.setEventType(EventType.CTR);
        e.setSourceSystem("AUTO_FROM_TXNS");
        e.setSourceEntityId("AGGREGATION");
        e.setExternalSubjectKey(subjectKey);
        e.setEventTime(day.atStartOfDay(ZoneOffset.UTC).toInstant());
        e.setTotalAmount(nullSafe(totalAmount));
        e.setStatus(EventStatus.CREATED);
        e.setSeverityScore(suspicionScore);
        e.setIdempotencyKey(idempotencyKey);

        // ✅ Persist subject type on the event itself (used by list views)
        if (sourceSubjectType != null && !sourceSubjectType.isBlank()) {
            try {
                e.setSourceSubjectType(SubjectType.valueOf(sourceSubjectType));
            } catch (IllegalArgumentException ignored) {
                // Leave null if unexpected value
            }
        }

        return e;
    }

    public ComplianceEventCtrDetail toCtrDetail(
            ComplianceEvent event,
            CtrAggregationRow row,
            List<Long> txnIds,
            Integer suspicionScore,
            String suspicionBand,
            List<String> suspicionDrivers) {

        ObjectNode formData = objectMapper.createObjectNode();
        formData.put("source", "AUTO_FROM_TXNS");
        formData.put("subjectKey", row.getSubjectKey());
        formData.put("txnDay", row.getTxnDay().toString());
        formData.put("totalCashIn", row.getTotalCashIn().toPlainString());
        formData.put("totalCashOut", row.getTotalCashOut().toPlainString());
        formData.put("totalCashAmount", row.getTotalCashAmount().toPlainString());
        formData.put("txnCount", row.getTxnCount());

        if (suspicionScore != null) {
            formData.put("suspicionScore", suspicionScore);
        }
        if (suspicionBand != null) {
            formData.put("suspicionBand", suspicionBand);
        }
        if (suspicionDrivers != null) {
            var dArr = formData.putArray("suspicionDrivers");
            suspicionDrivers.forEach(dArr::add);
        }

        var arr = formData.putArray("contributingTxnIds");
        txnIds.forEach(arr::add);

        ComplianceEventCtrDetail d = new ComplianceEventCtrDetail();

        // @MapsId relationship
        d.setEvent(event);

        // Subject name handling
        String subjectName = row.getSubjectName();
        String resolvedName = (subjectName == null || subjectName.isBlank())
                ? deriveCustomerName(row.getSubjectKey())
                : subjectName;

        d.setCustomerName(resolvedName);
        d.setTransactionTime(row.getTxnDay().atStartOfDay(ZoneOffset.UTC).toInstant());

        // Persist subject metadata into form data (audit + fallback)
        formData.put("subjectName", resolvedName);

        if (row.getSourceSubjectType() != null && !row.getSourceSubjectType().isBlank()) {
            formData.put("sourceSubjectType", row.getSourceSubjectType());
        }

        // ctr_form_data is Map<String,Object>
        @SuppressWarnings("unchecked")
        Map<String, Object> map = objectMapper.convertValue(formData, Map.class);
        d.setCtrFormData(map);

        return d;
    }

    private String deriveCustomerName(String subjectKey) {
        if (subjectKey == null || subjectKey.isBlank()) {
            return "UNKNOWN";
        }
        return subjectKey.length() > 128
                ? subjectKey.substring(0, 128)
                : subjectKey;
    }

    private BigDecimal nullSafe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
