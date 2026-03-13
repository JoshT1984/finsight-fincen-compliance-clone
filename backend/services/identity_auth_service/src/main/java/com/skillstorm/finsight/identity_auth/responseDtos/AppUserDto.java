package com.skillstorm.finsight.identity_auth.responseDtos;

public record AppUserDto(
        String firstName,
        String lastName,
        String phone,
        String email,
        String userId,
        Integer roleId,
        String roleName,
        String organizationId,
        String organizationName) {
}
