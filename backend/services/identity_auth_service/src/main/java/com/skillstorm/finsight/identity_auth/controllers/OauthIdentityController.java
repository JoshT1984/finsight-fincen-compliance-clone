package com.skillstorm.finsight.identity_auth.controllers;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpHeaders;
import com.skillstorm.finsight.identity_auth.requestDtos.LoginRequest;
import com.skillstorm.finsight.identity_auth.responseDtos.LoginResponse;
import com.skillstorm.finsight.identity_auth.services.OauthIdentityService;
import com.skillstorm.finsight.identity_auth.services.OauthStateService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/auth")
public class OauthIdentityController {

        private OauthIdentityService oauthIdentityService;
        private OauthStateService oauthStateService;

        public OauthIdentityController(OauthIdentityService oauthIdentityService, OauthStateService oauthStateService) {
                this.oauthIdentityService = oauthIdentityService;
                this.oauthStateService = oauthStateService;
        }

        @PostMapping("/login")
        public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest) {
                LoginResponse response = oauthIdentityService.loginWithRefresh(loginRequest.email(),
                                loginRequest.password());
                ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", response.refreshToken())
                                .httpOnly(true)
                                .path("/")
                                .maxAge(60 * 60 * 4) // 4 hours
                                .build();
                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                                .body(response);
        }

        @GetMapping("/oauth2/authorize/{provider}")
        public void startOAuth(
                        @PathVariable String provider,
                        @RequestParam String mode,
                        Authentication authentication,
                        HttpServletResponse response) throws IOException {

                String userId = null;

                // if ("link".equals(mode)) {
                // userId = authentication.getName(); // internal user ID
                // }

                // String state = oauthStateService.createState(mode, userId);

                response.sendRedirect(
                                "/oauth2/authorization/" + provider);
        }

        /**
         * Checks if the current user is connected with the specified provider.
         * Requires Authorization: Bearer <token> header.
         */
        @GetMapping("/oauth/linked/{provider}")
        public ResponseEntity<Boolean> isProviderLinked(
                        @PathVariable String provider,
                        Authentication authentication) {
                // Extract userId from authentication principal (assumes JWT subject is
                // userId)
                String userId = authentication.getName();
                boolean linked = oauthIdentityService.isProviderLinked(userId, provider);
                return ResponseEntity.ok(linked);
        }

        @PostMapping("/refresh")
        public ResponseEntity<LoginResponse> refresh(@CookieValue("refreshToken") String refreshToken) {
                LoginResponse response = oauthIdentityService.refreshAccessToken(refreshToken);
                ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", response.refreshToken())
                                .httpOnly(true)
                                .path("/")
                                .maxAge(60 * 60 * 4) // 4 hours
                                .build();
                return ResponseEntity.ok()
                                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                                .body(response);
        }

        @PostMapping("/logout")
        public ResponseEntity<Void> logout(@CookieValue("refreshToken") String refreshToken) {
                oauthIdentityService.revokeRefreshToken(refreshToken);
                ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                                .maxAge(0)
                                .httpOnly(true)
                                .path("/")
                                .build();
                return ResponseEntity.noContent()
                                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                                .build();
        }

        @PostMapping("/forgot-password")
        public ResponseEntity<Void> forgotPassword(@RequestBody Map<String, String> payload) {
                String email = payload.get("email");
                boolean sent = oauthIdentityService.sendPasswordResetEmail(email);
                // Always return 200 OK with generic message for security
                return ResponseEntity.ok().build();
        }

        /**
         * Reset password using a token and new password.
         * Expects JSON: { "token": "...", "newPassword": "..." }
         */
        @PostMapping("/reset-password")
        public ResponseEntity<Void> resetPassword(@RequestBody Map<String, String> payload) {
                String token = payload.get("token");
                String newPassword = payload.get("newPassword");
                if (token == null || newPassword == null) {
                        return ResponseEntity.badRequest().build();
                }
                boolean success = oauthIdentityService.resetPassword(token, newPassword);
                if (success) {
                        return ResponseEntity.ok().build();
                } else {
                        return ResponseEntity.status(403).build(); // Invalid or expired token
                }
        }

        @PostMapping("/oauth/link")
        public ResponseEntity<?> linkAccount(HttpServletRequest request, Authentication authentication) {
                System.out.println("Linking account for user: " + authentication.getName() + " Session ID: "
                                + request.getSession().getId());
                OAuth2AuthenticationToken oauth = (OAuth2AuthenticationToken) request.getSession()
                                .getAttribute("oauthToken");
                System.out.println("OAuth token from session: " + oauth);
                if (oauth == null) {
                        return ResponseEntity.badRequest().body("No OAuth info found in session.");
                }

                String provider = oauth.getAuthorizedClientRegistrationId();
                Map<String, Object> attributes = oauth.getPrincipal().getAttributes();
                String providerUserId = extractProviderUserId(provider, attributes);
                String providerEmail = (String) attributes.get("email");

                String appUserId = authentication.getName();

                oauthIdentityService.linkOAuthIdentity(appUserId, provider, providerUserId, providerEmail);

                return ResponseEntity.ok().build();
        }

        private String extractProviderUserId(String provider, Map<String, Object> attributes) {
                return switch (provider.toLowerCase()) {
                        case "google" -> (String) attributes.get("sub");
                        case "github" -> String.valueOf(attributes.get("id"));
                        default -> throw new IllegalStateException("Unsupported OAuth provider: " + provider);
                };
        }
}
