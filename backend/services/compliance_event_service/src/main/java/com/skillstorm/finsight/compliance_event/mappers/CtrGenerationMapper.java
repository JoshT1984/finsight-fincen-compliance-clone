package com.skillstorm.finsight.compliance_event.mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.skillstorm.finsight.compliance_event.models.ComplianceEvent;
import com.skillstorm.finsight.compliance_event.models.ComplianceEventCtrDetail;
import com.skillstorm.finsight.compliance_event.models.EventType;
import com.skillstorm.finsight.compliance_event.models.SubjectType;
import com.skillstorm.finsight.compliance_event.repositories.CtrAggregationRow;

@Component
public class CtrGenerationMapper {

    public static final String SOURCE_SYSTEM = "AUTO_FROM_TXNS_AGGREGATION";

    public String ctrIdempotencyKey(String subjectKey, LocalDate dayUtc) {
        return "CTR:" + subjectKey + ":" + dayUtc;
    }

    public ComplianceEvent toCtrEvent(CtrAggregationRow row) {
        LocalDate dayUtc = row.getTxnDay();
        String idempotencyKey = ctrIdempotencyKey(row.getSubjectKey(), dayUtc);

        ComplianceEvent ev = new ComplianceEvent();

        // Enums (matches your ComplianceEvent entity)
        ev.setEventType(EventType.CTR);
        // status will default in @PrePersist (CREATED for CTR), so we can omit
        // setStatus

        // deterministic eventTime: end-of-day UTC
        Instant eventTime = dayUtc.plusDays(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)
                .minusSeconds(1);
        ev.setEventTime(eventTime);

        // uniqueness / idempotency
        ev.setSourceSystem(SOURCE_SYSTEM);
        ev.setSourceEntityId(idempotencyKey);
        ev.setIdempotencyKey(idempotencyKey);

        // subject linkage
        ev.setExternalSubjectKey(row.getSubjectKey());
        ev.setSourceSubjectId(row.getSubjectKey());
        ev.setSourceSubjectType(parseSubjectType(row.getSourceSubjectType()));

        // amounts / scoring
        ev.setTotalAmount(row.getTotalCashAmount());
        ev.setSeverityScore(calcScore(row.getTotalCashAmount()));

        return ev;
    }

    public ComplianceEventCtrDetail toCtrDetail(ComplianceEvent savedCtrEvent, CtrAggregationRow row) {
        ComplianceEventCtrDetail d = new ComplianceEventCtrDetail();
        d.setEvent(savedCtrEvent);

        d.setCustomerName(row.getSubjectName());
        d.setTransactionTime(savedCtrEvent.getEventTime());

        Map<String, Object> form = new HashMap<>();
        form.put("subjectKey", row.getSubjectKey());
        form.put("subjectName", row.getSubjectName());
        form.put("sourceSubjectType", row.getSourceSubjectType());
        form.put("txnDay", row.getTxnDay() != null ? row.getTxnDay().toString() : null);
        form.put("totalCashIn", row.getTotalCashIn());
        form.put("totalCashOut", row.getTotalCashOut());
        form.put("totalCashAmount", row.getTotalCashAmount());
        form.put("txnCount", row.getTxnCount());
        form.put("score", savedCtrEvent.getSeverityScore());

        d.setCtrFormData(form);
        return d;
    }

    private SubjectType parseSubjectType(String raw) {
        if (raw == null || raw.isBlank())
            return null;
        try {
            return SubjectType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null; // don’t blow up CTR generation because of unknown type
        }
    }

    private int calcScore(BigDecimal totalCashAmount) {
        if (totalCashAmount == null)
            return 0;

        BigDecimal amt = totalCashAmount.abs();
        if (amt.compareTo(new BigDecimal("250000")) >= 0)
            return 100;
        if (amt.compareTo(new BigDecimal("100000")) >= 0)
            return 80;
        if (amt.compareTo(new BigDecimal("50000")) >= 0)
            return 60;
        if (amt.compareTo(new BigDecimal("25000")) >= 0)
            return 40;
        if (amt.compareTo(new BigDecimal("10000")) >= 0)
            return 20;
        return 0;
    }
}
