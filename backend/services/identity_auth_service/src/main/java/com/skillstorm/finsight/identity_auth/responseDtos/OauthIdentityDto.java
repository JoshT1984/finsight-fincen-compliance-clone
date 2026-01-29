package com.skillstorm.finsight.identity_auth.responseDtos;

public record OauthIdentityDto(
                String provider,
                String emailAtProvider,
                Boolean revoked) {
}
