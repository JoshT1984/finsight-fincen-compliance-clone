package com.skillstorm.finsight.suspect_registry.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.finsight.suspect_registry.models.Organization;
import com.skillstorm.finsight.suspect_registry.services.OrganizationService;

@RestController
@RequestMapping("/organization")
public class OrganizationController {
  
  private final OrganizationService organizationService;

  public OrganizationController(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  @GetMapping
  public ResponseEntity<List<Organization>> getAllOrganizations() {
    try {
      List<Organization> organizations = organizationService.getAllOrganizations();
      return ResponseEntity.ok(organizations);
    } catch (Exception e) {
      return ResponseEntity.status(500).build();
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<Organization> getOrganizationById(@PathVariable long id) {
    try {
      Organization organization = organizationService.getOrganizationById(id);
      if (organization != null) {
        return ResponseEntity.ok(organization);
      } else {
        return ResponseEntity.notFound().build();
      }
    } catch (Exception e) {
      return ResponseEntity.status(500).build();
    }
  }

  @PostMapping
  public ResponseEntity<Organization> createOrganization(Organization organization) {
    try {
      Organization createdOrganization = organizationService.createOrganization(organization);
      return ResponseEntity.status(201).body(createdOrganization);
    } catch (Exception e) {
      return ResponseEntity.status(500).build();
    }
  }
}