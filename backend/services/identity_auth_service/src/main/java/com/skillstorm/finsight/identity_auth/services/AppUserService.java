package com.skillstorm.finsight.identity_auth.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.skillstorm.finsight.identity_auth.exceptions.EmailAlreadyExistsException;
import com.skillstorm.finsight.identity_auth.exceptions.UserNotFoundException;
import com.skillstorm.finsight.identity_auth.models.AppUser;
import com.skillstorm.finsight.identity_auth.models.Role;
import com.skillstorm.finsight.identity_auth.repositories.AppUserRepository;
import com.skillstorm.finsight.identity_auth.requestDtos.AdminUpdateDto;
import com.skillstorm.finsight.identity_auth.requestDtos.ChangeEmailDto;
import com.skillstorm.finsight.identity_auth.requestDtos.ChangePasswordDto;
import com.skillstorm.finsight.identity_auth.requestDtos.UpdateUserDto;
import com.skillstorm.finsight.identity_auth.requestDtos.UserCreationDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AppUserService {

    private AppUserRepository appUserRepository;
    private PasswordEncoder passwordEncoder;
    private RoleService roleService;

    AppUserService(AppUserRepository appUserRepository, RoleService roleService, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AppUser> findAll() {
        return appUserRepository.findAll();
    }

    public Optional<AppUser> findById(String id) {
        return appUserRepository.findById(id);
    }

    public Optional<AppUser> findByEmail(String email) {
        return Optional.ofNullable(appUserRepository.findByEmail(email));
    }

    public AppUser createUser(UserCreationDto request, String roleName) {
        if (appUserRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already in use");
        }

        AppUser user = new AppUser();
        user.setUserId(UUID.randomUUID().toString());
        user.setEmail(request.getEmail());

        // **Hash the password before saving**
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        Role role = roleService.findByRoleName(roleName);
        user.setRole(role);

        return appUserRepository.save(user);
    }

    @Transactional
    public AppUser updateUser(String userId, UpdateUserDto user) {
        AppUser foundUser = findById(userId).orElseThrow(() -> new UserNotFoundException("User does not exist"));
        foundUser.setFirstName(user.getFirstName());
        foundUser.setLastName(user.getLastName());
        foundUser.setPhone(user.getPhone());
        return foundUser;
    }

    @Transactional
    public AppUser updateUserPassword(String userId, ChangePasswordDto passwordDto) {
        AppUser foundUser = findById(userId).orElseThrow(() -> new UserNotFoundException("User does not exist"));
        foundUser.setPasswordHash(passwordEncoder.encode(passwordDto.getNewPassword()));
        return foundUser;
    }

    @Transactional
    public AppUser updateUserEmail(String userId, ChangeEmailDto emailDto) {
        if (appUserRepository.existsByEmail(emailDto.getNewEmail())) {
            throw new EmailAlreadyExistsException("Email already in use");
        }
        AppUser foundUser = findById(userId).orElseThrow(() -> new UserNotFoundException("User does not exist"));
        foundUser.setEmail(emailDto.getNewEmail());
        return foundUser;
    }

    @Transactional
    public AppUser adminUpdate(String userId, AdminUpdateDto adminUpdateDto) {
        AppUser foundUser = findById(userId).orElseThrow(() -> new UserNotFoundException("User does not exist"));
        foundUser.setRole(roleService.findById(adminUpdateDto.getRoleId())
                .orElseThrow(() -> new RuntimeException("Role not found")));
        foundUser.setActive(adminUpdateDto.isActive());
        return foundUser;
    }

    public void deactivateUserById(String id) {
        appUserRepository.deactivateUserById(id);
    }
}
