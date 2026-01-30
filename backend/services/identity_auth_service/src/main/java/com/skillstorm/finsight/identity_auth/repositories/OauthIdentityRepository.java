package com.skillstorm.finsight.identity_auth.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skillstorm.finsight.identity_auth.models.OauthIdentity;

public interface OauthIdentityRepository extends JpaRepository<OauthIdentity, Long> {

    public List<OauthIdentity> findByEmailAtProvider(String email);

    public boolean existsByProviderAndProviderUserId(String provider, String providerUserId);
}
