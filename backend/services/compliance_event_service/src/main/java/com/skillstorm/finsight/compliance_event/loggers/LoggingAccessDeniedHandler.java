package com.skillstorm.finsight.compliance_event.loggers;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoggingAccessDeniedHandler
        implements AccessDeniedHandler {

    private final SecurityEventLogger securityLogger;

    public LoggingAccessDeniedHandler(SecurityEventLogger securityLogger) {
        this.securityLogger = securityLogger;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {

        securityLogger.authFailure(
                "ACCESS_DENIED",
                request,
                accessDeniedException);

        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}