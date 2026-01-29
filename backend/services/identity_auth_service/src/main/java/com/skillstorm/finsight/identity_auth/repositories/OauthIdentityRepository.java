package com.skillstorm.finsight.identity_auth.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.skillstorm.finsight.identity_auth.models.OauthIdentity;

public interface OauthIdentityRepository extends JpaRepository<OauthIdentity, Long> {
}
