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
import com.skillstorm.finsight.suspect_registry.emitters.SuspectRegistryEventEmitter;
import com.skillstorm.finsight.suspect_registry.exceptions.ResourceConflictException;
import com.skillstorm.finsight.suspect_registry.exceptions.ResourceNotFoundException;
import com.skillstorm.finsight.suspect_registry.loggers.SuspectRegistryEventLog;
import com.skillstorm.finsight.suspect_registry.models.Organization;
import com.skillstorm.finsight.suspect_registry.models.OrganizationType;
import com.skillstorm.finsight.suspect_registry.repositories.OrganizationRepository;
import com.skillstorm.finsight.suspect_registry.util.SecurityContextUtils;

import org.springframework.security.access.AccessDeniedException;

@Service
public class OrganizationService {

  private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);
  private static final OrganizationType DEFAULT_ORG_TYPE = OrganizationType.OTHER;

  private final OrganizationRepository repo;
  private final SuspectRegistryEventEmitter eventEmitter;

  public OrganizationService(OrganizationRepository repo, SuspectRegistryEventEmitter eventEmitter) {
    this.repo = repo;
    this.eventEmitter = eventEmitter;
  }

  private OrganizationResponse toResponse(Organization organization) {
    return new OrganizationResponse(
        organization.getId(),
        organization.getName(),
        organization.getType(),
        organization.getCreatedAt());
  }

  @Transactional
  public OrganizationResponse create(CreateOrganizationRequest request) {
    if (SecurityContextUtils.isComplianceUser()) {
      throw new AccessDeniedException("Compliance users have read-only access to the suspect registry");
    }
    log.debug("Creating organization: {}", request.name());

    if (repo.findByName(request.name()).isPresent()) {
      throw new ResourceConflictException("Organization with name " + request.name() + " already exists");
    }

    Organization org = new Organization();
    org.setName(request.name());
    org.setType(request.type() != null ? request.type() : DEFAULT_ORG_TYPE);
    Organization saved = repo.save(org);
    log.info("Created organization with ID: {}", saved.getId());
    eventEmitter.emit(
        SuspectRegistryEventLog.organizationCreated(
            String.valueOf(saved.getId()),
            "USER",
            java.util.Map.of(
                "name", saved.getName(),
                "type", saved.getType(),
                "actorUserId", SecurityContextUtils.getCurrentUserId().orElse(null))));
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
    if (SecurityContextUtils.isComplianceUser()) {
      throw new AccessDeniedException("Compliance users have read-only access to the suspect registry");
    }
    log.debug("Patching organization with ID: {}", orgId);
    Organization org = repo.findById(orgId)
        .orElseThrow(() -> new ResourceNotFoundException("Organization with ID " + orgId + " not found"));
    boolean updated = false;
    if (request.name() != null) {
      org.setName(request.name());
      updated = true;
    }
    if (request.type() != null) {
      org.setType(request.type());
      updated = true;
    }
    if (!updated) {
      log.warn("No fields to update for organization with ID: {}", orgId);
      return toResponse(org);
    }

    // Check for uniqueness if name is being updated
    if (request.name() != null) {
      repo.findByName(request.name()).ifPresent(existing -> {
        if (existing.getId() != orgId) {
          throw new ResourceConflictException("Organization with name " + request.name() + " already exists");
        }
      });
    }

    Organization saved = repo.save(org);
    log.info("Updated organization with ID: {}", saved.getId());
    eventEmitter.emit(
        SuspectRegistryEventLog.organizationUpdated(
            String.valueOf(saved.getId()),
            "USER",
            java.util.Map.of(
                "name", saved.getName(),
                "type", saved.getType(),
                "actorUserId", SecurityContextUtils.getCurrentUserId().orElse(null))));
    return toResponse(saved);
  }

  @Transactional
  public void deleteById(Long orgId) {
    if (SecurityContextUtils.isComplianceUser()) {
      throw new AccessDeniedException("Compliance users have read-only access to the suspect registry");
    }
    log.debug("Deleting organization with ID: {}", orgId);
    if (!repo.existsById(orgId)) {
      throw new ResourceNotFoundException("Organization with ID " + orgId + " not found");
    }
    Organization org = repo.findById(orgId).orElse(null);
    repo.deleteById(orgId);
    log.info("Deleted organization with ID: {}", orgId);
    if (org != null) {
      eventEmitter.emit(
          SuspectRegistryEventLog.organizationDeleted(
              String.valueOf(orgId),
              "USER",
              java.util.Map.of(
                  "name", org.getName(),
                  "type", org.getType(),
                  "actorUserId", SecurityContextUtils.getCurrentUserId().orElse(null))));
    }
  }
}
