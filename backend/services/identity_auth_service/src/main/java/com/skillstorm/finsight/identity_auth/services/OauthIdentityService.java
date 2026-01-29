package com.skillstorm.finsight.identity_auth.services;

import org.springframework.stereotype.Service;

import com.skillstorm.finsight.identity_auth.repositories.OauthIdentityRepository;

@Service
public class OauthIdentityService {

    private OauthIdentityRepository oauthIdentityRepository;

    public OauthIdentityService(OauthIdentityRepository oauthIdentityRepository) {
        this.oauthIdentityRepository = oauthIdentityRepository;
    }
}
