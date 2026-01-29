package com.skillstorm.finsight.identity_auth.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.skillstorm.finsight.identity_auth.models.OauthIdentity;
import com.skillstorm.finsight.identity_auth.services.OauthIdentityService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/oauth-identities")
public class OauthIdentityController {

    private OauthIdentityService oauthIdentityService;

    public OauthIdentityController(OauthIdentityService oauthIdentityService) {
        this.oauthIdentityService = oauthIdentityService;
    }

    @GetMapping
    public List<OauthIdentity> getAllOauthIdentities() {
        return oauthIdentityService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OauthIdentity> getOauthIdentityById(@PathVariable Long id) {
        Optional<OauthIdentity> oauthIdentity = oauthIdentityService.findById(id);
        return oauthIdentity.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public OauthIdentity createOauthIdentity(@RequestBody OauthIdentity oauthIdentity) {
        return oauthIdentityService.save(oauthIdentity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OauthIdentity> updateOauthIdentity(@PathVariable Long id,
            @RequestBody OauthIdentity details) {
        Optional<OauthIdentity> oauthOpt = oauthIdentityService.findById(id);
        if (oauthOpt.isEmpty())
            return ResponseEntity.notFound().build();
        OauthIdentity oauth = oauthOpt.get();
        oauth.setUser(details.getUser());
        oauth.setProvider(details.getProvider());
        oauth.setProviderUserId(details.getProviderUserId());
        oauth.setEmailAtProvider(details.getEmailAtProvider());
        oauth.setRevoked(details.isRevoked());
        oauth.setRevokedAt(details.getRevokedAt());
        return ResponseEntity.ok(oauthIdentityService.save(oauth));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOauthIdentity(@PathVariable Long id) {
        if (oauthIdentityService.findById(id).isEmpty())
            return ResponseEntity.notFound().build();
        oauthIdentityService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
