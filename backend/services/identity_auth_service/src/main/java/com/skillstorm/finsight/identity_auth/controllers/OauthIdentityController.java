package com.skillstorm.finsight.identity_auth.controllers;

import java.util.Base64;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

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
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest loginRequest) {
        String token = oauthIdentityService.login(loginRequest.email(), loginRequest.password());
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/oauth/link/{provider}")
    public ResponseEntity<Void> linkAccount(
            @PathVariable String provider,
            OAuth2AuthenticationToken auth,
            Authentication internalAuth) {
        System.out.println("Linking OAuth identity for provider: " + provider);
        String appUserId = internalAuth.getName();

        String providerUserId = auth.getPrincipal().getAttribute("sub");

        oauthIdentityService.linkOAuthIdentity(
                appUserId,
                provider,
                providerUserId);
        return ResponseEntity.noContent().build();
    }
}
