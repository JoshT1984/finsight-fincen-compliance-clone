package com.skillstorm.finsight.compliance_event.loggers;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class SecurityEventLogger {

    private static final Logger COMPLIANCE_SECURITY = LoggerFactory.getLogger("COMPLIANCE_SECURITY");

    private final ObjectMapper objectMapper;

    public SecurityEventLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void authFailure(
            String reason,
            HttpServletRequest request,
            Throwable error) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("timestamp", Instant.now().toString());
        event.put("eventType", "SECURITY");
        event.put("action", "AUTH_FAILURE");
        event.put("reason", reason);

        if (request != null) {
            event.put("ipAddress", request.getRemoteAddr());
            event.put("userAgent", request.getHeader("User-Agent"));
        }

        if (error != null) {
            event.put("errorType", error.getClass().getSimpleName());
        }

        try {
            COMPLIANCE_SECURITY.warn(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            COMPLIANCE_SECURITY.error("Failed to write security event", e);
        }
    }
}