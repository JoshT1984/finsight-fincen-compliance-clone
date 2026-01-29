package com.skillstorm.finsight.identity_auth.dtos;

import java.sql.Timestamp;

public class OauthIdentityDto {
    private String provider;
    private String providerUserId;
    private String emailAtProvider;
    private boolean revoked;
    private Timestamp revokedAt;
    private String userId;

    public OauthIdentityDto() {
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public void setProviderUserId(String providerUserId) {
        this.providerUserId = providerUserId;
    }

    public String getEmailAtProvider() {
        return emailAtProvider;
    }

    public void setEmailAtProvider(String emailAtProvider) {
        this.emailAtProvider = emailAtProvider;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Timestamp getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Timestamp revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
