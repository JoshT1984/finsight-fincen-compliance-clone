package com.skillstorm.finsight.compliance_event.config;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtEncoder jwtEncoder;

    public LoginSuccessHandler(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        Instant now = Instant.now();

        String userId = authentication.getName(); // username or subject
        String role = authentication.getAuthorities()
                .stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("USER");

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("identity-service")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .subject(userId)
                .claim("role", role)
                .build();

        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();

        response.setContentType("application/json");
        response.getWriter().write("""
                {
                  "accessToken": "%s"
                }
                """.formatted(token));
    }
}
