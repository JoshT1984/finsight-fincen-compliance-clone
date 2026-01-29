package com.skillstorm.finsight.identity_auth.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.finsight.identity_auth.exceptions.EmailAlreadyExistsException;
import com.skillstorm.finsight.identity_auth.exceptions.UserNotFoundException;
import com.skillstorm.finsight.identity_auth.models.AppUser;
import com.skillstorm.finsight.identity_auth.repositories.AppUserRepository;
import com.skillstorm.finsight.identity_auth.requestDtos.AdminUpdateDto;
import com.skillstorm.finsight.identity_auth.requestDtos.ChangeEmailDto;
import com.skillstorm.finsight.identity_auth.requestDtos.ChangePasswordDto;
import com.skillstorm.finsight.identity_auth.requestDtos.UpdateUserDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AppUserService {

    private AppUserRepository appUserRepository;
    private RoleService roleService;

    public AppUserService(AppUserRepository appUserRepository, RoleService roleService) {
        this.appUserRepository = appUserRepository;
        this.roleService = roleService;
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

    public AppUser createUser(AppUser user) {
        if (appUserRepository.existsByEmail(user.getEmail())) {
            throw new EmailAlreadyExistsException("Email already in use");
        }

        user.setUserId(UUID.randomUUID().toString());
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
        foundUser.setPasswordHash(passwordDto.getNewPassword());
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

    public void deleteById(String id) {
        appUserRepository.deleteAppUser(id);
    }
}
