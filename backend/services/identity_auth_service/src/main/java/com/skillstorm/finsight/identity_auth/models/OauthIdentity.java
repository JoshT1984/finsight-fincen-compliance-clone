package com.skillstorm.finsight.identity_auth.models;

import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "oauth_identity")
public class OauthIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth_id")
    private Long oauthId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 256)
    private String providerUserId;

    @Column(name = "email_at_provider", length = 320)
    private String emailAtProvider;

    @Column(name = "created_at", updatable = false, insertable = false)
    private Timestamp createdAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Timestamp revokedAt;

    public OauthIdentity() {
    }

    public OauthIdentity(Long oauthId, AppUser user, String provider, String providerUserId, String emailAtProvider,
            Timestamp createdAt, boolean revoked, Timestamp revokedAt) {
        this.oauthId = oauthId;
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.emailAtProvider = emailAtProvider;
        this.createdAt = createdAt;
        this.revoked = revoked;
        this.revokedAt = revokedAt;
    }

    // Getters and setters
    public Long getOauthId() {
        return oauthId;
    }

    public void setOauthId(Long oauthId) {
        this.oauthId = oauthId;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
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

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((oauthId == null) ? 0 : oauthId.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        result = prime * result + ((provider == null) ? 0 : provider.hashCode());
        result = prime * result + ((providerUserId == null) ? 0 : providerUserId.hashCode());
        result = prime * result + ((emailAtProvider == null) ? 0 : emailAtProvider.hashCode());
        result = prime * result + ((createdAt == null) ? 0 : createdAt.hashCode());
        result = prime * result + (revoked ? 1231 : 1237);
        result = prime * result + ((revokedAt == null) ? 0 : revokedAt.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OauthIdentity other = (OauthIdentity) obj;
        if (oauthId == null) {
            if (other.oauthId != null)
                return false;
        } else if (!oauthId.equals(other.oauthId))
            return false;
        if (user == null) {
            if (other.user != null)
                return false;
        } else if (!user.equals(other.user))
            return false;
        if (provider == null) {
            if (other.provider != null)
                return false;
        } else if (!provider.equals(other.provider))
            return false;
        if (providerUserId == null) {
            if (other.providerUserId != null)
                return false;
        } else if (!providerUserId.equals(other.providerUserId))
            return false;
        if (emailAtProvider == null) {
            if (other.emailAtProvider != null)
                return false;
        } else if (!emailAtProvider.equals(other.emailAtProvider))
            return false;
        if (createdAt == null) {
            if (other.createdAt != null)
                return false;
        } else if (!createdAt.equals(other.createdAt))
            return false;
        if (revoked != other.revoked)
            return false;
        if (revokedAt == null) {
            if (other.revokedAt != null)
                return false;
        } else if (!revokedAt.equals(other.revokedAt))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "OauthIdentity [oauthId=" + oauthId + ", user=" + user + ", provider=" + provider + ", providerUserId="
                + providerUserId + ", emailAtProvider=" + emailAtProvider + ", createdAt=" + createdAt + ", revoked="
                + revoked + ", revokedAt=" + revokedAt + "]";
    }

}
