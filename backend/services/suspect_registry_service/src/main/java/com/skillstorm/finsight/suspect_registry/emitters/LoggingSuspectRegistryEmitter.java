package com.skillstorm.finsight.suspect_registry.emitters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillstorm.finsight.suspect_registry.loggers.SuspectRegistryEventLog;

@Component
public class LoggingSuspectRegistryEmitter implements SuspectRegistryEventEmitter {

    private static final Logger DOCUMENT_LOG = LoggerFactory.getLogger("SUSPECT_REGISTRY_EVENT");

    private final ObjectMapper objectMapper;

    public LoggingSuspectRegistryEmitter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void emit(SuspectRegistryEventLog event) {
        try {
            DOCUMENT_LOG.info(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            // This is operational failure, not compliance data
            LoggerFactory.getLogger(getClass())
                    .error("Failed to emit document event", e);
        }
    }
}
