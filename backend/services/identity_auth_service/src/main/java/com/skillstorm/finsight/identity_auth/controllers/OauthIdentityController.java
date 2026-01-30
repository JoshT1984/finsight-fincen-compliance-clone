package com.skillstorm.finsight.identity_auth.controllers;

import org.springframework.web.bind.annotation.*;

import com.skillstorm.finsight.identity_auth.services.OauthIdentityService;

@RestController
@RequestMapping("/api/oauth-identities")
public class OauthIdentityController {

    private OauthIdentityService oauthIdentityService;

    public OauthIdentityController(OauthIdentityService oauthIdentityService) {
        this.oauthIdentityService = oauthIdentityService;
    }
}
