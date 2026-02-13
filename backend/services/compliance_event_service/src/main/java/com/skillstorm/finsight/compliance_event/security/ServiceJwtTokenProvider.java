package com.skillstorm.finsight.compliance_event.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class ServiceJwtTokenProvider {

    private final JwtEncoder jwtEncoder;

    private volatile String cachedToken;
    private volatile Instant cachedExp;

    @Value("${finsight.service-jwt.subject:service:compliance-event}")
    private String subject;

    // Use ANALYST so POST /api/cases and suspect-registry writes work with current
    // rules
    @Value("${finsight.service-jwt.role:ANALYST}")
    private String role;

    public ServiceJwtTokenProvider(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String getToken() {
        Instant now = Instant.now();

        // refresh if missing or expiring soon
        if (cachedToken != null && cachedExp != null && cachedExp.isAfter(now.plus(30, ChronoUnit.SECONDS))) {
            return cachedToken;
        }

        Instant exp = now.plus(10, ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("compliance-event-service")
                .issuedAt(now)
                .expiresAt(exp)
                .subject(subject)
                .claim("role", role)
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        cachedToken = token;
        cachedExp = exp;
        return token;
    }
}
