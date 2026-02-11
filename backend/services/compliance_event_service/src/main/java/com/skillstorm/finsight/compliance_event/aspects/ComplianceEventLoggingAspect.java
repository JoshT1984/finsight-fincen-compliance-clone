package com.skillstorm.finsight.compliance_event.aspects;

import com.skillstorm.finsight.compliance_event.enums.AuditOutcome;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ComplianceEventLoggingAspect {

    private final AuditLogger auditLogger;

    public ComplianceEventLoggingAspect(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    /* Logs only create, link, unlink, and generate actions */
    @Around("execution(public * com.skillstorm.finsight.compliance_event.services.ComplianceEventServiceImpl.create*(..)) || "
            +
            "execution(public * com.skillstorm.finsight.compliance_event.services.ComplianceEventServiceImpl.link*(..)) || "
            +
            "execution(public * com.skillstorm.finsight.compliance_event.services.ComplianceEventServiceImpl.unlink*(..)) || "
            +
            "execution(public * com.skillstorm.finsight.compliance_event.services.ComplianceEventServiceImpl.generate*(..))")
    public Object logComplianceEventAction(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result;
        try {
            result = joinPoint.proceed();
            auditLogger.logComplianceEvent(joinPoint, AuditOutcome.SUCCESS, result, null);
            return result;
        } catch (Exception ex) {
            auditLogger.logComplianceEvent(joinPoint, AuditOutcome.FAILURE, null, ex);
            throw ex;
        }
    }
}