package com.skillstorm.finsight.documents_cases.emitters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillstorm.finsight.documents_cases.loggers.DocumentEventLog;

@Component
public class LoggingDocumentEmitter implements DocumentEventEmitter {

    private static final Logger DOCUMENT_LOG = LoggerFactory.getLogger("DOCUMENT_EVENT");

    private final ObjectMapper objectMapper;

    public LoggingDocumentEmitter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void emit(DocumentEventLog event) {
        try {
            DOCUMENT_LOG.info(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            // This is operational failure, not compliance data
            LoggerFactory.getLogger(getClass())
                    .error("Failed to emit document event", e);
        }
    }
}
