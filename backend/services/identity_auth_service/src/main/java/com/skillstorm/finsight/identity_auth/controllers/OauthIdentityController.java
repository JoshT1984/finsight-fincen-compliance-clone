package com.skillstorm.finsight.identity_auth.controllers;

import java.util.Base64;
import java.util.Map;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import com.skillstorm.finsight.identity_auth.models.OauthIdentity;
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
}
