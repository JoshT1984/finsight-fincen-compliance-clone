package com.skillstorm.finsight.identity_auth.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.skillstorm.finsight.identity_auth.models.AppUser;
import com.skillstorm.finsight.identity_auth.models.OauthIdentity;
import com.skillstorm.finsight.identity_auth.repositories.OauthIdentityRepository;
import com.skillstorm.finsight.identity_auth.responseDtos.LoginResponse;

@Service
public class OauthIdentityService {

    private final OauthIdentityRepository oauthIdentityRepository;
    private final JwtEncoder jwtEncoder;
    private final AppUserService appUserService;
    private final PasswordEncoder passwordEncoder;

    public OauthIdentityService(OauthIdentityRepository oauthIdentityRepository, JwtEncoder jwtEncoder,
            PasswordEncoder passwordEncoder, AppUserService appUserService) {
        this.oauthIdentityRepository = oauthIdentityRepository;
        this.jwtEncoder = jwtEncoder;
        this.passwordEncoder = passwordEncoder;
        this.appUserService = appUserService;
    }

    public LoginResponse login(String email, String password) {
        var user = appUserService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = generateToken(user.getUserId(), user.getRole().getRoleName());
        return new LoginResponse(token, user.getRole().getRoleName());
    }

    public void linkOAuthIdentity(
            String appUserId,
            String provider,
            String providerUserId) {
        // 1️⃣ Verify internal user exists
        AppUser user = appUserService.findById(appUserId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        // 2️⃣ Ensure OAuth identity is not already linked
        boolean alreadyLinked = oauthIdentityRepository
                .existsByProviderAndProviderUserId(provider, providerUserId);

        if (alreadyLinked) {
            throw new IllegalStateException(
                    "This OAuth account is already linked to another user");
        }

        // 4️⃣ Persist link
        OauthIdentity identity = new OauthIdentity();
        identity.setProvider(provider);
        identity.setProviderUserId(providerUserId);
        identity.setUser(user);

        oauthIdentityRepository.save(identity);
    }

    public String generateToken(String userId, String role) {
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
}
