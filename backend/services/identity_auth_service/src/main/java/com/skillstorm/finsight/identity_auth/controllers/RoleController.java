package com.skillstorm.finsight.identity_auth.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.skillstorm.finsight.identity_auth.models.Role;
import com.skillstorm.finsight.identity_auth.services.RoleService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public List<Role> getAllRoles() {
        return roleService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Role> getRoleById(@PathVariable Integer id) {
        Optional<Role> role = roleService.findById(id);
        return role.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public Role createRole(@RequestBody Role role) {
        return roleService.save(role);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Role> updateRole(@PathVariable Integer id, @RequestBody Role roleDetails) {
        Optional<Role> roleOpt = roleService.findById(id);
        if (roleOpt.isEmpty())
            return ResponseEntity.notFound().build();
        Role role = roleOpt.get();
        role.setRoleName(roleDetails.getRoleName());
        return ResponseEntity.ok(roleService.save(role));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Integer id) {
        if (roleService.findById(id).isEmpty())
            return ResponseEntity.notFound().build();
        roleService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
