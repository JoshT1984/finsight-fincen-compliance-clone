package com.skillstorm.finsight.identity_auth.controllers;

import com.skillstorm.finsight.identity_auth.models.Organization;
import com.skillstorm.finsight.identity_auth.services.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orgs")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @GetMapping
    public List<Organization> getAllOrganizations() {
        return organizationService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Organization> getOrganizationById(@PathVariable String id) {
        return organizationService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Organization> createOrganization(@RequestBody Organization organization) {
        Organization saved = organizationService.save(organization);
        return ResponseEntity.created(URI.create("/api/orgs/" + saved.getOrganizationId())).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Organization> updateOrganization(@PathVariable String id,
            @RequestBody Organization organization) {
        Optional<Organization> existing = organizationService.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        organization.setOrganizationId(id);
        Organization updated = organizationService.save(organization);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganization(@PathVariable String id) {
        if (organizationService.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        organizationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
