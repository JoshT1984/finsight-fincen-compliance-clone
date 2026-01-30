package com.skillstorm.finsight.suspect_registry.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.finsight.suspect_registry.dtos.request.CreateOrganizationRequest;
import com.skillstorm.finsight.suspect_registry.dtos.request.PatchOrganizationRequest;
import com.skillstorm.finsight.suspect_registry.dtos.response.OrganizationResponse;
import com.skillstorm.finsight.suspect_registry.services.OrganizationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {
  
  private final OrganizationService service;

  public OrganizationController(OrganizationService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<OrganizationResponse> create(@Valid @RequestBody CreateOrganizationRequest request) {
    OrganizationResponse response = service.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  public ResponseEntity<List<OrganizationResponse>> getAll() {
    List<OrganizationResponse> organizations = service.findAll();
    return ResponseEntity.ok(organizations);
  }

  @GetMapping("/{id}")
  public ResponseEntity<OrganizationResponse> getById(@PathVariable Long id) {
    OrganizationResponse response = service.findById(id);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}")
  public ResponseEntity<OrganizationResponse> update(@PathVariable Long id, @Valid @RequestBody PatchOrganizationRequest request) {
    OrganizationResponse response = service.updateById(id, request);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}