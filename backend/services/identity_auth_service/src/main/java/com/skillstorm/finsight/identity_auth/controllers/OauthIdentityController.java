
package com.skillstorm.finsight.identity_auth.controllers;

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

@RestController
@RequestMapping("/auth")
public class OauthIdentityController {

        private OauthIdentityService oauthIdentityService;

        public OauthIdentityController(OauthIdentityService oauthIdentityService) {
                this.oauthIdentityService = oauthIdentityService;
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

        @PostMapping("/oauth/link/{provider}")
        public ResponseEntity<Void> linkAccount(
                        @PathVariable String provider,
                        OAuth2AuthenticationToken auth,
                        Authentication internalAuth) {
                String appUserId = internalAuth.getName();

                String providerUserId = auth.getPrincipal().getAttribute("sub");

                oauthIdentityService.linkOAuthIdentity(
                                appUserId,
                                provider,
                                providerUserId);
                return ResponseEntity.noContent().build();
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
}
