package com.skillstorm.finsight.identity_auth.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.skillstorm.finsight.identity_auth.enums.AuditAction;
import com.skillstorm.finsight.identity_auth.enums.AuditOutcome;

@Aspect
@Component
public class AppUserAuditAspect {
    private final AuditLogger auditLogger;

    public AppUserAuditAspect(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @AfterReturning(pointcut = "execution(* com.skillstorm.finsight.identity_auth.services.AppUserService.*(..))", returning = "result")
    public void success(JoinPoint joinPoint, Object result) {
        AuditAction action = resolveAction(joinPoint);
        if (action != null) {
            auditLogger.log(joinPoint, action, AuditOutcome.SUCCESS, result, null);
        }
    }

    @AfterThrowing(pointcut = "execution(* com.skillstorm.finsight.identity_auth.services.AppUserService.*(..))", throwing = "ex")
    public void failure(JoinPoint joinPoint, Throwable ex) {
        AuditAction action = resolveAction(joinPoint);
        if (action != null) {
            auditLogger.log(joinPoint, action, AuditOutcome.FAILURE, null, ex);
        }
    }

    private AuditAction resolveAction(JoinPoint joinPoint) {
        String method = ((MethodSignature) joinPoint.getSignature())
                .getMethod()
                .getName()
                .toLowerCase();

        if (method.startsWith("create"))
            return AuditAction.CREATE;
        if (method.startsWith("update"))
            return AuditAction.UPDATE;
        if (method.startsWith("delete"))
            return AuditAction.DEACTIVATE;
        return null;
    }
}
