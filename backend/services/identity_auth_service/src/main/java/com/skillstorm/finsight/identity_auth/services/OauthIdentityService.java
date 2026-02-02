package com.skillstorm.finsight.identity_auth.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.authentication.BadCredentialsException;
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

    // In-memory store for refresh tokens (replace with persistent store in
    // production)
    private final java.util.Map<String, String> refreshTokenStore = new java.util.concurrent.ConcurrentHashMap<>();

    public LoginResponse loginWithRefresh(String email, String password) {
        AppUser user = appUserService.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String accessToken = generateToken(user.getUserId(), user.getRole().getRoleName());
        String refreshToken = java.util.UUID.randomUUID().toString();
        refreshTokenStore.put(refreshToken, user.getUserId());
        return new com.skillstorm.finsight.identity_auth.responseDtos.LoginResponse(accessToken,
                user.getRole().getRoleName(), refreshToken);
    }

    public LoginResponse refreshAccessToken(String refreshToken) {
        String userId = refreshTokenStore.get(refreshToken);
        if (userId == null) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        AppUser user = appUserService.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        String accessToken = generateToken(user.getUserId(), user.getRole().getRoleName());
        return new com.skillstorm.finsight.identity_auth.responseDtos.LoginResponse(accessToken,
                user.getRole().getRoleName(), refreshToken);
    }

    public void revokeRefreshToken(String refreshToken) {
        refreshTokenStore.remove(refreshToken);
    }

    public void linkOAuthIdentity(
            String appUserId,
            String provider,
            String providerUserId) {

        AppUser user = appUserService.findById(appUserId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        boolean alreadyLinked = oauthIdentityRepository
                .existsByProviderAndProviderUserId(provider, providerUserId);

        if (alreadyLinked) {
            throw new IllegalStateException(
                    "This OAuth account is already linked to another user");
        }

        OauthIdentity identity = new OauthIdentity();
        identity.setProvider(provider);
        identity.setProviderUserId(providerUserId);
        identity.setUser(user);
        identity.setEmailAtProvider(user.getEmail());

        oauthIdentityRepository.save(identity);
    }

    public String generateToken(String userId, String role) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("identity-service")
                .issuedAt(now)
                .expiresAt(now.plus(15, ChronoUnit.MINUTES))
                .subject(userId)
                .claim("role", role)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
