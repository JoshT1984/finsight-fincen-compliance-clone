package com.skillstorm.finsight.compliance_event.loggers;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoggingAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final SecurityEventLogger securityLogger;

    public LoggingAuthenticationEntryPoint(SecurityEventLogger securityLogger) {
        this.securityLogger = securityLogger;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        securityLogger.authFailure(
                "AUTHENTICATION_FAILED",
                request,
                authException);

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}