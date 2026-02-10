package com.skillstorm.finsight.identity_auth.aspects;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillstorm.finsight.identity_auth.enums.AuditAction;
import com.skillstorm.finsight.identity_auth.enums.AuditOutcome;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuditLogger {

    private final ObjectMapper objectMapper;
    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT_LOG");

    public AuditLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void log(
            JoinPoint joinPoint,
            AuditAction action,
            AuditOutcome outcome,
            Object result,
            Throwable ex) {
        System.out.println("AUDIT LOGGER ENTERED");
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = (auth != null) ? auth.getName() : "anonymous";

            Object entity = (result != null)
                    ? result
                    : (joinPoint.getArgs().length > 0 ? joinPoint.getArgs()[0] : null);

            String targetEntityType = entity != null
                    ? entity.getClass().getSimpleName()
                    : null;

            Object targetUserId = extractUserId(entity);

            Map<String, Object> audit = new LinkedHashMap<>();
            audit.put("userId", userId);
            audit.put("action", action.name());
            audit.put("outcome", outcome.name());
            audit.put("targetEntityType", targetEntityType);
            audit.put("targetId", targetUserId);

            if (outcome == AuditOutcome.FAILURE && ex != null) {
                audit.put("errorType", ex.getClass().getSimpleName());
            }

            AUDIT_LOG.info(objectMapper.writeValueAsString(audit));
        } catch (Exception e) {
            AUDIT_LOG.warn("Failed to write audit log", e);
            e.printStackTrace();
        }
    }

    private Object extractUserId(Object entity) {
        if (entity == null)
            return null;

        try {
            return entity.getClass()
                    .getMethod("getUserId")
                    .invoke(entity);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void logSecurityEvent(
            String actorUserId,
            AuditAction action,
            AuditOutcome outcome,
            String provider,
            HttpServletRequest request,
            Throwable error) {
        try {
            Map<String, Object> audit = new HashMap<>();
            audit.put("actorUserId", actorUserId != null ? actorUserId : "anonymous");
            audit.put("action", action.name());
            audit.put("outcome", outcome.name());
            audit.put("provider", provider != null ? provider : "unknown");

            if (request != null) {
                audit.put("ipAddress", request.getRemoteAddr());
                audit.put("userAgent", request.getHeader("User-Agent"));
            }

            if (error != null) {
                audit.put("errorType", error.getClass().getSimpleName());
            }

            String json = objectMapper.writeValueAsString(audit);
            AUDIT_LOG.info(json);

        } catch (Exception e) {
            AUDIT_LOG.warn("Failed to write security audit log", e);
        }
    }
}