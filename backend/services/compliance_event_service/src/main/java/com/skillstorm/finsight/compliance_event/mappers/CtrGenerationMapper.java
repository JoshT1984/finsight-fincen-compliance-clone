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
import com.skillstorm.finsight.compliance_event.repositories.CtrAggregationRow;

@Component
public class CtrGenerationMapper {

    private final ObjectMapper objectMapper;

    public CtrGenerationMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ComplianceEvent toCtrEvent(
            String subjectKey,
            LocalDate day,
            BigDecimal totalAmount,
            String idempotencyKey
    ) {
        ComplianceEvent e = new ComplianceEvent();
        e.setEventType(EventType.CTR);
        e.setSourceSystem("AUTO_FROM_TXNS");
        e.setSourceEntityId("AGGREGATION");
        e.setExternalSubjectKey(subjectKey);
        e.setEventTime(day.atStartOfDay(ZoneOffset.UTC).toInstant());
        e.setTotalAmount(nullSafe(totalAmount));
        e.setStatus(EventStatus.CREATED);
        e.setSeverityScore(null);
        e.setIdempotencyKey(idempotencyKey);
        return e;
    }

    public ComplianceEventCtrDetail toCtrDetail(
            ComplianceEvent event,
            CtrAggregationRow row,
            List<Long> txnIds
    ) {
        ObjectNode formData = objectMapper.createObjectNode();
        formData.put("source", "AUTO_FROM_TXNS");
        formData.put("subjectKey", row.getSubjectKey());
        formData.put("txnDay", row.getTxnDay().toString());
        formData.put("totalCashIn", row.getTotalCashIn().toPlainString());
        formData.put("totalCashOut", row.getTotalCashOut().toPlainString());
        formData.put("totalCashAmount", row.getTotalCashAmount().toPlainString());
        formData.put("txnCount", row.getTxnCount());

        var arr = formData.putArray("contributingTxnIds");
        txnIds.forEach(arr::add);

        ComplianceEventCtrDetail d = new ComplianceEventCtrDetail();

        // Your entity uses @MapsId with field name "event" and setter setEvent(...)
        d.setEvent(event);

        // Required NOT NULL columns in compliance_event_ctr_detail
        d.setCustomerName(deriveCustomerName(row.getSubjectKey()));
        d.setTransactionTime(row.getTxnDay().atStartOfDay(ZoneOffset.UTC).toInstant());

        // ctr_form_data is Map<String,Object> (JSON)
        @SuppressWarnings("unchecked")
        Map<String, Object> map = objectMapper.convertValue(formData, Map.class);
        d.setCtrFormData(map);

        return d;
    }

    private String deriveCustomerName(String subjectKey) {
        if (subjectKey == null || subjectKey.isBlank()) {
            return "UNKNOWN";
        }
        return subjectKey.length() > 128 ? subjectKey.substring(0, 128) : subjectKey;
    }

    private BigDecimal nullSafe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
