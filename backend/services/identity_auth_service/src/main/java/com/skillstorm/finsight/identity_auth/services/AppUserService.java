package com.skillstorm.finsight.identity_auth.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.finsight.identity_auth.exceptions.EmailAlreadyExistsException;
import com.skillstorm.finsight.identity_auth.exceptions.PasswordNotStrongEnoughException;
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

import java.util.regex.Pattern;

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

    // Password strength regex: at least 8 chars, 1 upper, 1 lower, 1 digit, 1
    // special char
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).{8,}$");

    private void validatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        if (!STRONG_PASSWORD_PATTERN.matcher(password).matches()) {
            StringBuilder error = new StringBuilder(
                    "Password must be at least 8 characters, include upper and lower case letters, a digit, and a special character.");
            if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
                error.append(" Password must contain at least one special character.");
            }
            throw new PasswordNotStrongEnoughException(error.toString());
        }
    }

    public List<AppUser> findAll() {
        return appUserRepository.findAll();
    }

    /**
     * Returns the current user by userId (from Authentication.getName()).
     */
    public Optional<AppUser> getCurrentUser(String userId) {
        return findById(userId);
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
        // Validate email format
        if (request.getEmail() == null || !request.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{3,}$")) {
            throw new IllegalArgumentException("Email must be in the format user@domain.xxx");
        }

        AppUser user = new AppUser();
        user.setUserId(UUID.randomUUID().toString());
        user.setEmail(request.getEmail());

        // Validate password strength before hashing
        validatePasswordStrength(request.getPassword());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        Role role = roleService.findByRoleName(roleName);
        user.setRole(role);

        return appUserRepository.save(user);
    }

    @Transactional
    public AppUser updateUser(String userId, UpdateUserDto user) {
        AppUser foundUser = findById(userId).orElseThrow(() -> new UserNotFoundException("User does not exist"));
        // Validate firstName and lastName
        if (user.getFirstName() == null || user.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be empty");
        }
        if (user.getLastName() == null || user.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be empty");
        }
        // Validate email format
        if (user.getEmail() == null || !user.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{3,}$")) {
            throw new IllegalArgumentException("Email must be in the format user@domain.xxx");
        }
        foundUser.setFirstName(user.getFirstName());
        foundUser.setLastName(user.getLastName());
        foundUser.setPhone(user.getPhone());
        foundUser.setEmail(user.getEmail());
        return foundUser;
    }

    @Transactional
    public AppUser updateUserPassword(String userId, ChangePasswordDto passwordDto) {
        AppUser foundUser = findById(userId).orElseThrow(() -> new UserNotFoundException("User does not exist"));
        // Validate password strength before hashing
        validatePasswordStrength(passwordDto.getNewPassword());
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

    // @Transactional
    // public AppUser adminUpdate(String userId, AdminUpdateDto adminUpdateDto) {
    // AppUser foundUser = findById(userId).orElseThrow(() -> new
    // UserNotFoundException("User does not exist"));
    // foundUser.setRole(roleService.findById(adminUpdateDto.getRoleId())
    // .orElseThrow(() -> new RuntimeException("Role not found")));
    // foundUser.setActive(adminUpdateDto.isActive());
    // return foundUser;
    // }

    public void deactivateUserById(String id) {
        appUserRepository.deactivateUserById(id);
    }
}
