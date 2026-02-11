package com.skillstorm.finsight.identity_auth.aspects;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
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

    private static final Logger IDENTITY_SECURITY = LoggerFactory.getLogger("IDENTITY_SECURITY");

    private final ObjectMapper objectMapper;
    private final String serviceName = "identity-service";

    public AuditLogger(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /*
     * =======================
     * Public entry points
     * =======================
     */

    public void log(
            JoinPoint joinPoint,
            AuditAction action,
            AuditOutcome outcome,
            Object result,
            Throwable ex) {

        Object target = resolveTarget(joinPoint, result);
        String intent = resolveIntent(joinPoint);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        writeAuditEvent(
                action,
                outcome,
                auth != null ? auth.getName() : null,
                null,
                null,
                target,
                intent,
                ex);
    }

    public void logSecurityEvent(
            String actorUserId,
            AuditAction action,
            AuditOutcome outcome,
            String provider,
            HttpServletRequest request,
            Throwable error) {

        writeAuditEvent(
                action,
                outcome,
                actorUserId,
                provider,
                request,
                null,
                null,
                error);
    }

    /*
     * =======================
     * Core audit writer
     * =======================
     */

    private void writeAuditEvent(
            AuditAction action,
            AuditOutcome outcome,
            String actorUserId,
            String provider,
            HttpServletRequest request,
            Object target,
            String operation,
            Throwable error) {

        try {
            Map<String, Object> audit = new LinkedHashMap<>();

            audit.put("timestamp", Instant.now().toString());
            audit.put("level", outcome == AuditOutcome.FAILURE ? "ERROR" : "INFO");
            audit.put("service", serviceName);
            audit.put("eventType", "AUTH");

            audit.put("action", action.name());
            audit.put("outcome", outcome.name());
            audit.put("actorUserId", actorUserId != null ? actorUserId : "anonymous");

            Map<String, Object> auth = new LinkedHashMap<>();

            if (provider != null)
                auth.put("provider", provider);

            if (request != null) {
                auth.put("ipAddress", request.getRemoteAddr());
                auth.put("userAgent", request.getHeader("User-Agent"));
            }

            if (!auth.isEmpty()) {
                audit.put("auth", auth);
            }

            if (operation != null) {
                audit.put("operation", operation);
            }

            audit.put("target", buildTarget(target));
            enrichError(audit, error);

            IDENTITY_SECURITY.info(objectMapper.writeValueAsString(audit));

        } catch (Exception e) {
            IDENTITY_SECURITY.warn("Failed to write audit log", e);
        }
    }

    /*
     * =======================
     * Helpers
     * =======================
     */

    private Object resolveTarget(JoinPoint joinPoint, Object result) {
        if (result != null)
            return result;

        Object[] args = joinPoint.getArgs();
        return args.length > 0 ? args[0] : null;
    }

    private Map<String, Object> buildTarget(Object entity) {
        if (entity == null)
            return null;

        Map<String, Object> target = new LinkedHashMap<>();
        target.put("type", entity.getClass().getSimpleName());
        target.put("id", extractUserId(entity));
        return target;
    }

    private void enrichError(Map<String, Object> audit, Throwable error) {
        if (error == null)
            return;

        Map<String, Object> err = new LinkedHashMap<>();
        err.put("type", error.getClass().getSimpleName());
        err.put("message", error.getMessage());

        audit.put("error", err);
    }

    private Object extractUserId(Object entity) {
        try {
            return entity.getClass()
                    .getMethod("getUserId")
                    .invoke(entity);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveIntent(JoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        AuditIntent intent = method.getAnnotation(AuditIntent.class);
        return intent != null ? intent.value() : null;
    }
}