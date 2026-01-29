package com.skillstorm.finsight.identity_auth.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.finsight.identity_auth.dtos.AppUserDto;
import com.skillstorm.finsight.identity_auth.models.AppUser;
import com.skillstorm.finsight.identity_auth.repositories.AppUserRepository;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class AppUserService {

    private AppUserRepository appUserRepository;

    public AppUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
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
        if (appUserRepository.findByUserId(user.getUserId()) != null) {
            throw new IllegalArgumentException("User already exists");
        }
        if (findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }
        return appUserRepository.save(user);
    }

    @Transactional
    public AppUser updateUser(String userId, AppUserDto user) {
        Optional<AppUser> foundUser = findById(userId);
        if (foundUser.isEmpty()) {
            throw new UserNotFoundException("User does not exist");
        }
        foundUser.get().setEmail(user.getEmail());
        foundUser.get().setPasswordHash(user.getPasswordHash());
        foundUser.get().setFirstName(user.getFirstName());
        foundUser.get().setLastName(user.getLastName());
        foundUser.get().setPhone(user.getPhone());
        foundUser.get().setActive(user.isActive());
        return foundUser.get();
    }

    public void deleteById(String id) {
        appUserRepository.deleteAppUser(id);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Timestamp.from(Instant.now());
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Timestamp.from(Instant.now());
    }
}
