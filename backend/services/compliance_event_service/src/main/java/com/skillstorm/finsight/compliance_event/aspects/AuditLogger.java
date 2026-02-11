package com.skillstorm.finsight.compliance_event.aspects;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillstorm.finsight.compliance_event.enums.AuditAction;
import com.skillstorm.finsight.compliance_event.enums.AuditOutcome;
import com.skillstorm.finsight.compliance_event.models.ComplianceEvent;
import com.skillstorm.finsight.compliance_event.repositories.CtrAggregationRow;
import com.skillstorm.finsight.compliance_event.repositories.CtrRiskSignalsRow;
import com.skillstorm.finsight.compliance_event.services.CtrSuspicionScoring;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuditLogger {

    private final ObjectMapper objectMapper;
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT_LOG");

    public AuditLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void logComplianceEvent(JoinPoint joinPoint, AuditOutcome outcome, Object result, Throwable ex) {
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("timestamp", Instant.now().toString());
        logMap.put("action", joinPoint.getSignature().getName().toUpperCase());
        logMap.put("outcome", outcome.name());

        if (result instanceof ComplianceEvent event) {
            logMap.put("eventId", event.getEventId());
            logMap.put("eventType", event.getEventType());
            logMap.put("sourceSystem", event.getSourceSystem());
            logMap.put("subjectKey", event.getExternalSubjectKey());
            logMap.put("amount", event.getTotalAmount());
            logMap.put("severityScore", event.getSeverityScore());
            logMap.put("suspectId",
                    event.getSuspectSnapshot() != null ? event.getSuspectSnapshot().getSuspectId() : null);
        }

        if (ex != null) {
            logMap.put("error", ex.getMessage());
        }

        AUDIT_LOG.info(asJsonString(logMap));
    }

    public void logCtrGeneration(ProceedingJoinPoint joinPoint, int created, AuditOutcome outcome) {
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("timestamp", Instant.now().toString());
        logMap.put("action", joinPoint.getSignature().getName().toUpperCase());
        logMap.put("outcome", outcome.name());

        Object[] args = joinPoint.getArgs();
        if (args.length == 2 && args[0] instanceof LocalDate from && args[1] instanceof LocalDate to) {
            logMap.put("from", from.toString());
            logMap.put("to", to.toString());
        } else if (args.length == 2 && args[0] instanceof String subjectKey && args[1] instanceof LocalDate day) {
            logMap.put("subjectKey", subjectKey);
            logMap.put("day", day.toString());
        }

        logMap.put("created", created);

        AUDIT_LOG.info(asJsonString(logMap));
    }

    /* Helper method to turn a map into a JSON string */
    private String asJsonString(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        map.forEach((k, v) -> {
            if (v instanceof String) {
                sb.append("\"").append(k).append("\":\"").append(v).append("\",");
            } else {
                sb.append("\"").append(k).append("\":").append(v).append(",");
            }
        });
        if (sb.charAt(sb.length() - 1) == ',')
            sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }
}