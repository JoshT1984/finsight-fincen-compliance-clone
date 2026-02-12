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
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getEmail(),
                user.getUserId(), // String
                (user.getRole() != null ? user.getRole().getRoleId() : null),
                (user.getRole() != null ? user.getRole().getRoleName() : null),
                (user.getOrganization() != null ? user.getOrganization().getName() : null));
    }

    public AppUser toEntity(AppUserDto dto) {

        AppUser user = new AppUser();

        user.setUserId(dto.userId()); // String matches model
        user.setEmail(dto.email());
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setPhone(dto.phone());

        Role role = roleRepository.findByRoleName(dto.roleName());
        if (role == null) {
            throw new IllegalArgumentException("Role not found: " + dto.roleName());
        }

        user.setRole(role);

        return user;
    }
}
