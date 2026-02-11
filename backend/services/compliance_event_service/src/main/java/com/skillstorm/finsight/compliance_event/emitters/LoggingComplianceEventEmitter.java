package com.skillstorm.finsight.compliance_event.emitters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillstorm.finsight.compliance_event.loggers.ComplianceEventLog;

@Component
public class LoggingComplianceEventEmitter implements ComplianceEventEmitter {

    private static final Logger COMPLIANCE_LOG = LoggerFactory.getLogger("COMPLIANCE_LOG");

    private final ObjectMapper objectMapper;

    public LoggingComplianceEventEmitter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void emit(ComplianceEventLog event) {
        try {
            COMPLIANCE_LOG.info(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            // This is operational failure, not compliance data
            LoggerFactory.getLogger(getClass())
                    .error("Failed to emit compliance event", e);
        }
    }
}
