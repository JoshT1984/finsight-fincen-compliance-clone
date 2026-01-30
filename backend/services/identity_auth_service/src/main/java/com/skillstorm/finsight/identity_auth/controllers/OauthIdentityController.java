package com.skillstorm.finsight.identity_auth.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(oauthIdentityService.login(request.email(), request.password()));
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
}
