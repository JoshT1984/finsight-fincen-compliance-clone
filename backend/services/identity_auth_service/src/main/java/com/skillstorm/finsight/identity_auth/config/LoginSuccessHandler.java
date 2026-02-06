package com.skillstorm.finsight.identity_auth.config;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.skillstorm.finsight.identity_auth.models.AppUser;
import com.skillstorm.finsight.identity_auth.services.OauthIdentityService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

        @Value("${frontend.frontend_url}")
        private String frontendUrl;

        private final JwtEncoder jwtEncoder;
        private final OauthIdentityService oauthIdentityService;

        public LoginSuccessHandler(JwtEncoder jwtEncoder, OauthIdentityService oauthIdentityService) {
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

                        String provider = oauth.getAuthorizedClientRegistrationId();
                        Map<String, Object> attributes = oauth.getPrincipal().getAttributes();
                        String providerUserId = extractProviderUserId(provider, attributes);

                        // 🔹 Check if provider is already linked

                        String appUserId = oauthIdentityService.findUserId(provider, providerUserId);

                        if (appUserId != null) {
                                // Existing provider → login
                                handleOAuthLogin(appUserId, response);
                        } else {
                                // New provider → redirect to link account page
                                // Store the OAuth token in session so /linkAccount can access it
                                request.getSession().setAttribute("oauthToken", oauth);
                                response.sendRedirect(frontendUrl + "/linkAccount");
                        }
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
        private void handleInternalLogin(Authentication authentication, HttpServletResponse response)
                        throws IOException {
                String userId = authentication.getName();
                String role = authentication.getAuthorities()
                                .stream()
                                .findFirst()
                                .map(GrantedAuthority::getAuthority)
                                .orElse("USER");

                issueJwt(userId, role, response);
                response.sendRedirect(frontendUrl + "/profile");
        }

        /*
         * =========================================================
         * ==================== OAUTH LOGIN
         * =========================================================
         */
        private void handleOAuthLogin(String appUserId, HttpServletResponse response) throws IOException {
                // Fetch the user and their role
                AppUser user = oauthIdentityService.getAppUserById(appUserId);
                String role = user.getRole().getRoleName();
                String token = issueJwt(appUserId, role);
                response.sendRedirect(frontendUrl + "/oauth-callback?accessToken=" + token);
        }

        /*
         * =========================================================
         * ==================== JWT CREATION
         * =========================================================
         */
        private void issueJwt(String userId, String role, HttpServletResponse response) throws IOException {

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

        private String issueJwt(String userId, String role) {
                Instant now = Instant.now();
                JwtClaimsSet claims = JwtClaimsSet.builder()
                                .issuer("identity-service")
                                .issuedAt(now)
                                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                                .subject(userId)
                                .claim("role", role)
                                .build();

                return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        }

        /*
         * =========================================================
         * ==================== HELPERS
         * =========================================================
         */
        private String extractProviderUserId(String provider, Map<String, Object> attributes) {
                return switch (provider.toLowerCase()) {
                        case "google" -> (String) attributes.get("sub");
                        case "github" -> String.valueOf(attributes.get("id"));
                        default -> throw new IllegalStateException("Unsupported OAuth provider: " + provider);
                };
        }
}