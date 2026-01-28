package com.skillstorm.finsight.suspect_registry.services;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.finsight.suspect_registry.dtos.request.CreateOrganizationRequest;
import com.skillstorm.finsight.suspect_registry.dtos.request.PatchOrganizationRequest;
import com.skillstorm.finsight.suspect_registry.dtos.response.OrganizationResponse;
import com.skillstorm.finsight.suspect_registry.exceptions.ResourceNotFoundException;
import com.skillstorm.finsight.suspect_registry.models.Organization;
import com.skillstorm.finsight.suspect_registry.repositories.OrganizationRepository;

@Service
public class OrganizationService {
  
  private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);
  
  private final OrganizationRepository repo;

  public OrganizationService(OrganizationRepository repo) {
    this.repo = repo;
  }

  private OrganizationResponse toResponse(Organization organization) {
    return new OrganizationResponse(
        organization.getId(),
        organization.getName(),
        organization.getType(),
        organization.getCreatedAt()
    );
  }

  @Transactional
  public OrganizationResponse create(CreateOrganizationRequest request) {
    log.debug("Creating organization: {}", request.name());
    Organization org = new Organization();
    org.setName(request.name());
    org.setType(request.type());
    Organization saved = repo.save(org);
    log.info("Created organization with ID: {}", saved.getId());
    return toResponse(saved);
  }

  public List<OrganizationResponse> findAll() {
    log.debug("Retrieving all organizations");
    return repo.findAll().stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  public OrganizationResponse findById(Long orgId) {
    log.debug("Retrieving organization with ID: {}", orgId);
    
    Organization organization = repo.findById(orgId)
        .orElseThrow(() -> new ResourceNotFoundException("Organization with ID " + orgId + " not found"));
    
    return toResponse(organization);
  }

  @Transactional
  public OrganizationResponse updateById(Long orgId, PatchOrganizationRequest request) {
    log.debug("Patching organization with ID: {}", orgId);
    Organization org = repo.findById(orgId)
        .orElseThrow(() -> new ResourceNotFoundException("Organization with ID " + orgId + " not found"));
    boolean updated = false;
    if (request.name() != null) { org.setName(request.name()); updated = true; }
    if (request.type() != null) { org.setType(request.type()); updated = true; }
    if (!updated) {
      log.warn("No fields to update for organization with ID: {}", orgId);
      return toResponse(org);
    }
    Organization saved = repo.save(org);
    log.info("Updated organization with ID: {}", saved.getId());
    return toResponse(saved);
  }

  @Transactional
  public void deleteById(Long orgId) {
    log.debug("Deleting organization with ID: {}", orgId);
    if (!repo.existsById(orgId)) {
      throw new ResourceNotFoundException("Organization with ID " + orgId + " not found");
    }
    repo.deleteById(orgId);
    log.info("Deleted organization with ID: {}", orgId);
  }
}
