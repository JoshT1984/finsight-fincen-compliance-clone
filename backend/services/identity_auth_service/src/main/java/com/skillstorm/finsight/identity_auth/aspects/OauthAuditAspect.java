package com.skillstorm.finsight.identity_auth.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.skillstorm.finsight.identity_auth.enums.AuditAction;
import com.skillstorm.finsight.identity_auth.enums.AuditOutcome;
import com.skillstorm.finsight.identity_auth.responseDtos.LoginResponse;
import com.skillstorm.finsight.identity_auth.services.OauthIdentityService;

@Aspect
@Component
public class OauthAuditAspect {
    private final AuditLogger auditLogger;

    public OauthAuditAspect(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    @AfterReturning(pointcut = "execution(* com.skillstorm.finsight.identity_auth.services.OauthIdentityService.loginWithRefresh(..))", returning = "result")
    public void loginSuccess(JoinPoint joinPoint, Object result) {
        if (result instanceof LoginResponse loginResponse) {
            String userId = loginResponse.userId(); // correct internal ID
            auditLogger.logSecurityEvent(
                    userId,
                    AuditAction.LOGIN,
                    AuditOutcome.SUCCESS,
                    "internal",
                    null,
                    null);
        }
    }

    @AfterReturning(pointcut = "execution(* com.skillstorm.finsight.identity_auth.services.OauthIdentityService.*(..))", returning = "result")
    public void success(JoinPoint joinPoint, Object result) {
        AuditAction action = resolveAction(joinPoint);
        if (action != null) {
            auditLogger.log(joinPoint, action, AuditOutcome.SUCCESS, result, null);
        }
    }

    @AfterThrowing(pointcut = "execution(* com.skillstorm.finsight.identity_auth.services.OauthIdentityService.*(..))", throwing = "ex")
    public void failure(JoinPoint joinPoint, Throwable ex) {
        AuditAction action = resolveAction(joinPoint);
        if (action != null) {
            auditLogger.log(joinPoint, action, AuditOutcome.FAILURE, null, ex);
        }
    }

    private AuditAction resolveAction(JoinPoint joinPoint) {
        String methodName = ((MethodSignature) joinPoint.getSignature())
                .getMethod().getName().toLowerCase();

        if (methodName.startsWith("link"))
            return AuditAction.LINK;
        if (methodName.startsWith("sendpassword"))
            return AuditAction.PASSWORD_RESET_EMAIL;
        if (methodName.startsWith("resetpassword"))
            return AuditAction.PASSWORD_RESET;
        if (methodName.startsWith("revoke"))
            return AuditAction.LOGOUT;

        return null;
    }
}
