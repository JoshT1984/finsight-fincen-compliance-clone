package com.skillstorm.finsight.identity_auth.services;

import org.springframework.stereotype.Service;

import com.skillstorm.finsight.identity_auth.models.OauthIdentity;
import com.skillstorm.finsight.identity_auth.repositories.OauthIdentityRepository;

import java.util.List;
import java.util.Optional;

@Service
public class OauthIdentityService {

    private OauthIdentityRepository oauthIdentityRepository;

    public OauthIdentityService(OauthIdentityRepository oauthIdentityRepository) {
        this.oauthIdentityRepository = oauthIdentityRepository;
    }

    public List<OauthIdentity> findAll() {
        return oauthIdentityRepository.findAll();
    }

    public Optional<OauthIdentity> findById(Long id) {
        return oauthIdentityRepository.findById(id);
    }

    public OauthIdentity save(OauthIdentity oauthIdentity) {
        return oauthIdentityRepository.save(oauthIdentity);
    }

    public void deleteById(Long id) {
        oauthIdentityRepository.deleteById(id);
    }
}
