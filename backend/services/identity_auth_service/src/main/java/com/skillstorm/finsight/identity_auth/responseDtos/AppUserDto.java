package com.skillstorm.finsight.identity_auth.responseDtos;

public record AppUserDto(
                String userId,
                String email,
                String firstName,
                String lastName,
                String phone,
                String roleName) {
}
