package com.skillstorm.finsight.identity_auth.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.skillstorm.finsight.identity_auth.models.AppUser;
import com.skillstorm.finsight.identity_auth.services.AppUserService;
import com.skillstorm.finsight.identity_auth.requestDtos.UpdateUserDto;
import com.skillstorm.finsight.identity_auth.requestDtos.UserCreationDto;
import com.skillstorm.finsight.identity_auth.requestDtos.ChangePasswordDto;
import com.skillstorm.finsight.identity_auth.requestDtos.ChangeEmailDto;
import com.skillstorm.finsight.identity_auth.responseDtos.AppUserDto;
import com.skillstorm.finsight.identity_auth.mappers.AppUserMapper;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class AppUserController {

    private AppUserService appUserService;

    public AppUserController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping
    public ResponseEntity<List<AppUserDto>> getAllUsers() {
        List<AppUser> users = appUserService.findAll();
        List<AppUserDto> dtos = users.stream().map(AppUserMapper::toDto).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppUserDto> getUserById(@PathVariable String id) {
        Optional<AppUser> user = appUserService.findById(id);
        return user.map(u -> ResponseEntity.ok(AppUserMapper.toDto(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/me")
    public ResponseEntity<AppUserDto> getCurrentUser(Authentication authentication) {
        String userId = authentication.getName();
        Optional<AppUser> user = appUserService.findById(userId);
        return user.map(u -> ResponseEntity.ok(AppUserMapper.toDto(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/me")
    public ResponseEntity<AppUserDto> updateCurrentUser(
            Authentication authentication,
            @RequestBody UpdateUserDto updateUserDto) {
        String userId = authentication.getName();
        Optional<AppUser> userOpt = appUserService.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        AppUser updated = appUserService.updateUser(userId, updateUserDto);
        return ResponseEntity.ok(AppUserMapper.toDto(updated));
    }

    @PutMapping("/me/password")
    public ResponseEntity<AppUserDto> updateCurrentUserPassword(
            Authentication authentication,
            @RequestBody ChangePasswordDto passwordDto) {
        String userId = authentication.getName();
        System.out.println(passwordDto);
        Optional<AppUser> userOpt = appUserService.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        AppUser updated = appUserService.updateUserPassword(userId, passwordDto);
        return ResponseEntity.ok(AppUserMapper.toDto(updated));
    }

    @PostMapping("/analyst")
    public ResponseEntity<AppUserDto> createAnalyst(@RequestBody UserCreationDto user) {
        AppUser created = appUserService.createUser(user, "ANALYST");
        return ResponseEntity.ok(AppUserMapper.toDto(created));
    }

    @PostMapping("/compliance-user")
    public ResponseEntity<AppUserDto> createComplianceUser(@RequestBody UserCreationDto user) {
        AppUser created = appUserService.createUser(user, "COMPLIANCE_USER");
        return ResponseEntity.ok(AppUserMapper.toDto(created));
    }

    @PostMapping("/law-enforcement-user")
    public ResponseEntity<AppUserDto> createLawEnforcementUser(@RequestBody UserCreationDto user) {
        AppUser created = appUserService.createUser(user, "LAW_ENFORCEMENT_USER");
        return ResponseEntity.ok(AppUserMapper.toDto(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppUserDto> updateUser(@PathVariable String id, @RequestBody UpdateUserDto userDto) {
        AppUser updated = appUserService.updateUser(id, userDto);
        return ResponseEntity.ok(AppUserMapper.toDto(updated));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<AppUserDto> updateUserPassword(@PathVariable String id,
            @RequestBody ChangePasswordDto passwordDto) {
        AppUser updated = appUserService.updateUserPassword(id, passwordDto);
        return ResponseEntity.ok(AppUserMapper.toDto(updated));
    }

    @PutMapping("/{id}/email")
    public ResponseEntity<AppUserDto> updateUserEmail(@PathVariable String id, @RequestBody ChangeEmailDto emailDto) {
        AppUser updated = appUserService.updateUserEmail(id, emailDto);
        return ResponseEntity.ok(AppUserMapper.toDto(updated));
    }

    // @PutMapping("/{id}/admin")
    // public ResponseEntity<AppUserDto> adminUpdate(@PathVariable String id,
    // @RequestBody AdminUpdateDto adminUpdateDto) {
    // AppUser updated = appUserService.adminUpdate(id, adminUpdateDto);
    // return ResponseEntity.ok(AppUserMapper.toDto(updated));
    // }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateUser(@PathVariable String id) {
        if (appUserService.findById(id).isEmpty())
            return ResponseEntity.notFound().build();
        appUserService.deactivateUserById(id);
        return ResponseEntity.noContent().build();
    }
}
