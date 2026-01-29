package com.skillstorm.finsight.identity_auth.mappers;

import com.skillstorm.finsight.identity_auth.models.OauthIdentity;
import com.skillstorm.finsight.identity_auth.responseDtos.OauthIdentityDto;

public class OauthMapper {

    public static OauthIdentityDto toDto(OauthIdentity entity) {
        if (entity == null)
            return null;
        return new OauthIdentityDto(
                entity.getProvider(),
                entity.getEmailAtProvider(),
                entity.isRevoked());
    }

    public static OauthIdentity toEntity(OauthIdentityDto dto) {
        if (dto == null)
            return null;
        OauthIdentity entity = new OauthIdentity();
        entity.setProvider(dto.provider());
        entity.setEmailAtProvider(dto.emailAtProvider());
        entity.setRevoked(dto.revoked() != null ? dto.revoked() : false);
        return entity;
    }
}
