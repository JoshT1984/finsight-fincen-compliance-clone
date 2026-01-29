package com.skillstorm.finsight.identity_auth.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.skillstorm.finsight.identity_auth.models.AppUser;
import com.skillstorm.finsight.identity_auth.services.AppUserService;

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
    public ResponseEntity<List<AppUser>> getAllUsers() {
        List<AppUser> users = appUserService.findAll();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppUser> getUserById(@PathVariable String id) {
        Optional<AppUser> user = appUserService.findById(id);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AppUser> createUser(@RequestBody AppUser user) {
        AppUser created = appUserService.createUser(user);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppUser> updateUser(@PathVariable String id, @RequestBody AppUser userDetails) {
        Optional<AppUser> userOpt = appUserService.findById(id);
        if (userOpt.isEmpty())
            return ResponseEntity.notFound().build();
        AppUser user = userOpt.get();
        user.setEmail(userDetails.getEmail());
        user.setPasswordHash(userDetails.getPasswordHash());
        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setPhone(userDetails.getPhone());
        user.setActive(userDetails.isActive());
        user.setRole(userDetails.getRole());
        return ResponseEntity.ok(appUserService.save(user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        if (appUserService.findById(id).isEmpty())
            return ResponseEntity.notFound().build();
        appUserService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
