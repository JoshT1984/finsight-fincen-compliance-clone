package com.skillstorm.finsight.identity_auth.aspects;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import com.skillstorm.finsight.identity_auth.enums.AuditAction;
import com.skillstorm.finsight.identity_auth.enums.AuditOutcome;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class SecurityAuditLogger {
    private final AuditLogger auditLogger;

    public SecurityAuditLogger(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    public void logLoginSuccess(
            HttpServletRequest request,
            Authentication authentication,
            String provider) {
        auditLogger.logSecurityEvent(
                authentication.getName(),
                AuditAction.LOGIN,
                AuditOutcome.SUCCESS,
                provider,
                request,
                null);
    }

    public void logLoginSuccess(
            String appUserId,
            HttpServletRequest request,
            Authentication authentication,
            String provider) {
        auditLogger.logSecurityEvent(
                appUserId,
                AuditAction.LOGIN,
                AuditOutcome.SUCCESS,
                provider,
                request,
                null);
    }

    public void logLoginFailure(
            HttpServletRequest request,
            AuthenticationException ex,
            String provider) {
        auditLogger.logSecurityEvent(
                "anonymous",
                AuditAction.LOGIN,
                AuditOutcome.FAILURE,
                provider,
                request,
                ex);
    }
}