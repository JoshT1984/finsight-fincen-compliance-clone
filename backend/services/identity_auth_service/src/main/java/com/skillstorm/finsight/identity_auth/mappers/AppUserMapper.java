package com.skillstorm.finsight.identity_auth.mappers;

import com.skillstorm.finsight.identity_auth.models.AppUser;
import com.skillstorm.finsight.identity_auth.models.Role;
import com.skillstorm.finsight.identity_auth.repositories.RoleRepository;
import com.skillstorm.finsight.identity_auth.responseDtos.AppUserDto;

public class AppUserMapper {

    private final RoleRepository roleRepository;

    public AppUserMapper(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public static AppUserDto toDto(AppUser user) {
        return new AppUserDto(
                user.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole() != null ? user.getRole().getRoleName() : null);
    }

    public AppUser toEntity(AppUserDto dto) {
        AppUser user = new AppUser();
        user.setUserId(dto.userId());
        user.setEmail(dto.email());
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setPhone(dto.phone());

        Role role = roleRepository.findByName(dto.roleName());
        if (role == null) {
            throw new IllegalArgumentException("Role not found: " + dto.roleName());
        }
        user.setRole(role);

        return user;
    }
}
