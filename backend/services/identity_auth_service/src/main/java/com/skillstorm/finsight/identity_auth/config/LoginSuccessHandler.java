package com.skillstorm.finsight.identity_auth.config;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.skillstorm.finsight.identity_auth.services.OauthIdentityService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

        private final JwtEncoder jwtEncoder;
        private final OauthIdentityService oauthIdentityService;

        public LoginSuccessHandler(
                        JwtEncoder jwtEncoder,
                        OauthIdentityService oauthIdentityService) {

                this.jwtEncoder = jwtEncoder;
                this.oauthIdentityService = oauthIdentityService;
        }

        @Override
        public void onAuthenticationSuccess(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        Authentication authentication) throws IOException {

                // 🔹 CASE 1: OAuth-based authentication
                if (authentication instanceof OAuth2AuthenticationToken oauth) {

                        String mode = extractMode(request);

                        if ("link".equals(mode)) {
                                handleOAuthLinking(oauth, request, response);
                                return;
                        }

                        // Default OAuth behavior = login
                        handleOAuthLogin(oauth, response);
                        return;
                }

                // 🔹 CASE 2: Normal username/password login
                handleInternalLogin(authentication, response);
        }

        /*
         * =========================================================
         * =============== INTERNAL LOGIN (EMAIL/PASS)
         * =========================================================
         */

        private void handleInternalLogin(
                        Authentication authentication,
                        HttpServletResponse response) throws IOException {

                String userId = authentication.getName();
                String role = authentication.getAuthorities()
                                .stream()
                                .findFirst()
                                .map(GrantedAuthority::getAuthority)
                                .orElse("USER");

                issueJwt(userId, role, response);
        }

        /*
         * =========================================================
         * ==================== OAUTH LOGIN
         * =========================================================
         */

        private void handleOAuthLogin(
                        OAuth2AuthenticationToken oauth,
                        HttpServletResponse response) throws IOException {

                String provider = oauth.getAuthorizedClientRegistrationId();
                Map<String, Object> attributes = oauth.getPrincipal().getAttributes();

                String providerUserId = extractProviderUserId(provider, attributes);

                // 🔹 Map OAuth identity → internal user
                String appUserId = oauthIdentityService.findUserId(provider, providerUserId);

                issueJwt(appUserId, "USER", response);
        }

        /*
         * =========================================================
         * ==================== OAUTH LINKING
         * =========================================================
         */

        private void handleOAuthLinking(
                        OAuth2AuthenticationToken oauth,
                        HttpServletRequest request,
                        HttpServletResponse response) throws IOException {

                String appUserId = extractUserIdFromState(request);

                String provider = oauth.getAuthorizedClientRegistrationId();
                Map<String, Object> attributes = oauth.getPrincipal().getAttributes();

                String providerUserId = extractProviderUserId(provider, attributes);
                String providerEmail = (String) attributes.get("email");

                oauthIdentityService.linkOAuthIdentity(
                                appUserId,
                                provider,
                                providerUserId,
                                providerEmail);

                // 🔹 No JWT issued here
                response.sendRedirect("http://localhost:4200/settings?linked=success");
        }

        /*
         * =========================================================
         * ==================== JWT CREATION
         * =========================================================
         */

        private void issueJwt(
                        String userId,
                        String role,
                        HttpServletResponse response) throws IOException {

                Instant now = Instant.now();

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

        /*
         * =========================================================
         * ==================== HELPERS
         * =========================================================
         */

        private String extractMode(HttpServletRequest request) {
                // TODO: decode from OAuth "state" parameter
                return request.getParameter("mode"); // placeholder
        }

        private String extractUserIdFromState(HttpServletRequest request) {
                // TODO: decode userId from OAuth "state"
                return request.getParameter("userId"); // placeholder
        }

        private String extractProviderUserId(
                        String provider,
                        Map<String, Object> attributes) {

                return switch (provider.toLowerCase()) {
                        case "google" -> (String) attributes.get("sub");
                        case "github" -> String.valueOf(attributes.get("id"));
                        default -> throw new IllegalStateException(
                                        "Unsupported OAuth provider: " + provider);
                };
        }
}

// @Override
// public void onAuthenticationSuccess(
// HttpServletRequest request,
// HttpServletResponse response,
// Authentication authentication) throws IOException {

// Instant now = Instant.now();

// String userId = authentication.getName(); // username or subject
// String role = authentication.getAuthorities()
// .stream()
// .findFirst()
// .map(GrantedAuthority::getAuthority)
// .orElse("USER");

// JwtClaimsSet claims = JwtClaimsSet.builder()
// .issuer("identity-service")
// .issuedAt(now)
// .expiresAt(now.plus(1, ChronoUnit.HOURS))
// .subject(userId)
// .claim("role", role)
// .build();

// String token = jwtEncoder
// .encode(JwtEncoderParameters.from(claims))
// .getTokenValue();

// response.setContentType("application/json");
// response.getWriter().write("""
// {
// "accessToken": "%s"
// }
// """.formatted(token));
// }
