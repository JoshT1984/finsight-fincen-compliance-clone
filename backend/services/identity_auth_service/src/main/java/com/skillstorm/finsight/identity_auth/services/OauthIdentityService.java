
package com.skillstorm.finsight.identity_auth.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;

import com.skillstorm.finsight.identity_auth.models.AppUser;
import com.skillstorm.finsight.identity_auth.models.OauthIdentity;
import com.skillstorm.finsight.identity_auth.repositories.OauthIdentityRepository;
import com.skillstorm.finsight.identity_auth.requestDtos.ChangePasswordDto;
import com.skillstorm.finsight.identity_auth.responseDtos.LoginResponse;

@Service
public class OauthIdentityService {

    private final OauthIdentityRepository oauthIdentityRepository;
    private final JwtEncoder jwtEncoder;

    private final AppUserService appUserService;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    public OauthIdentityService(OauthIdentityRepository oauthIdentityRepository, JwtEncoder jwtEncoder,
            PasswordEncoder passwordEncoder, AppUserService appUserService, JavaMailSender mailSender) {
        this.oauthIdentityRepository = oauthIdentityRepository;
        this.jwtEncoder = jwtEncoder;
        this.passwordEncoder = passwordEncoder;
        this.appUserService = appUserService;
        this.mailSender = mailSender;
    }

    // In-memory store for refresh tokens (replace with persistent store in
    // production)
    private final java.util.Map<String, String> refreshTokenStore = new java.util.concurrent.ConcurrentHashMap<>();

    // In-memory store for password reset tokens: token -> (userId, expiration)
    private final java.util.Map<String, ResetTokenInfo> resetTokenStore = new java.util.concurrent.ConcurrentHashMap<>();

    private static class ResetTokenInfo {
        String userId;
        Instant expiresAt;

        ResetTokenInfo(String userId, Instant expiresAt) {
            this.userId = userId;
            this.expiresAt = expiresAt;
        }
    }

    public AppUser getAppUserById(String userId) {
        return appUserService.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }

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
                user.getUserId(), refreshToken);
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
                user.getUserId(), refreshToken);
    }

    public void revokeRefreshToken(String refreshToken) {
        refreshTokenStore.remove(refreshToken);
    }

    public void linkOAuthIdentity(
            String appUserId,
            String provider,
            String providerUserId,
            String providerEmail) {

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
        identity.setEmailAtProvider(providerEmail);

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

    // Real password reset email logic
    public boolean sendPasswordResetEmail(String email) {
        AppUser user = null;
        try {
            user = appUserService.findByEmail(email).orElse(null);
            if (user == null) {
                // Do not reveal if user exists
                return false;
            }
            // Generate a reset token and store it with expiration (15 min)
            String resetToken = java.util.UUID.randomUUID().toString();
            Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);
            resetTokenStore.put(resetToken, new ResetTokenInfo(user.getUserId(), expiresAt));

            // Send email with real reset link (replace with your frontend URL)
            String resetUrl = "http://localhost:4200/reset-password?token=" + resetToken;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setFrom("matthew.wright9630@gmail.com");
            message.setSubject("Password Reset Request");
            message.setText("You requested a password reset. Use this link to reset your password: " + resetUrl
                    + "\n\nThis link will expire in 15 minutes.");
            mailSender.send(message);

            return true;
        } catch (Exception e) {
            // Log the exception to console for debugging
            System.err.println("Exception in sendPasswordResetEmail: " + e.getMessage());
            e.printStackTrace();
            // Do not reveal error details to client
            return false;
        }
    }

    /**
     * Validate a password reset token and return the userId if valid, else null.
     */
    public String validateResetToken(String token) {
        ResetTokenInfo info = resetTokenStore.get(token);
        if (info == null)
            return null;
        if (Instant.now().isAfter(info.expiresAt)) {
            resetTokenStore.remove(token);
            return null;
        }
        return info.userId;
    }

    /**
     * Consume a password reset token (removes it from the store).
     */
    public void consumeResetToken(String token) {
        resetTokenStore.remove(token);
    }

    /**
     * Reset the user's password using a valid token.
     */
    public boolean resetPassword(String token, String newPassword) {
        String userId = validateResetToken(token);
        if (userId == null)
            return false;
        AppUser user = appUserService.findById(userId).orElse(null);
        if (user == null)
            return false;
        ChangePasswordDto passwordDto = new ChangePasswordDto();
        passwordDto.setNewPassword(newPassword);
        appUserService.updateUserPassword(user.getUserId(), passwordDto);
        consumeResetToken(token);
        return true;
    }

    /**
     * Checks if the given user is connected with the specified provider.
     */
    public boolean isProviderLinked(String userId, String provider) {
        return oauthIdentityRepository.existsByUserUserIdAndProvider(userId, provider);
    }

    public String findUserId(String provider, String providerUserId) {
        OauthIdentity identity = oauthIdentityRepository.findByProviderAndProviderUserId(provider, providerUserId);

        if (identity == null || identity.getUser() == null) {
            return null; // not linked yet
        }

        return identity.getUser().getUserId();
    }
}
