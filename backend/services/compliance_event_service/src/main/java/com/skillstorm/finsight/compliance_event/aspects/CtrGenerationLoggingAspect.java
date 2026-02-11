package com.skillstorm.finsight.compliance_event.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.skillstorm.finsight.compliance_event.enums.AuditOutcome;

@Aspect
@Component
public class CtrGenerationLoggingAspect {

    private final AuditLogger auditLogger;

    public CtrGenerationLoggingAspect(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @Around("execution(public * com.skillstorm.finsight.compliance_event.services.CtrGenerationService.create*(..)) || "
            + "execution(public * com.skillstorm.finsight.compliance_event.services.CtrGenerationService.update*(..)) || "
            + "execution(public * com.skillstorm.finsight.compliance_event.services.CtrGenerationService.delete*(..)) || "
            + "execution(public * com.skillstorm.finsight.compliance_event.services.CtrGenerationService.generate*(..))")
    public Object logCtrGenerationAction(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result;
        try {
            result = joinPoint.proceed();

            if (result instanceof Integer count) {
                auditLogger.logCtrGeneration(joinPoint, count, AuditOutcome.SUCCESS);
            } else {
                auditLogger.logCtrGeneration(joinPoint, 0, AuditOutcome.SUCCESS);
            }

            return result;
        } catch (Exception ex) {
            auditLogger.logCtrGeneration(joinPoint, 0, AuditOutcome.FAILURE);
            throw ex;
        }
    }
}