package com.skillstorm.finsight.suspect_registry.services;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.finsight.suspect_registry.dtos.request.CreateAliasRequest;
import com.skillstorm.finsight.suspect_registry.dtos.request.PatchAliasRequest;
import com.skillstorm.finsight.suspect_registry.dtos.response.AliasResponse;
import com.skillstorm.finsight.suspect_registry.exceptions.ResourceConflictException;
import com.skillstorm.finsight.suspect_registry.exceptions.ResourceNotFoundException;
import com.skillstorm.finsight.suspect_registry.models.Alias;
import com.skillstorm.finsight.suspect_registry.models.AliasType;
import com.skillstorm.finsight.suspect_registry.models.Suspect;
import com.skillstorm.finsight.suspect_registry.repositories.AliasRepository;
import com.skillstorm.finsight.suspect_registry.repositories.SuspectRepository;
import com.skillstorm.finsight.suspect_registry.util.SecurityContextUtils;

import org.springframework.security.access.AccessDeniedException;

@Service
public class AliasService {
  
  private static final Logger log = LoggerFactory.getLogger(AliasService.class);
  private static final AliasType DEFAULT_ALIAS_TYPE = AliasType.AKA;
  
  private final AliasRepository repo;
  private final SuspectRepository suspectRepo;
  
  public AliasService(AliasRepository repo, SuspectRepository suspectRepo) {
    this.repo = repo;
    this.suspectRepo = suspectRepo;
  }

  private AliasResponse toResponse(Alias alias) {
    return new AliasResponse(
        alias.getId(),
        alias.getSuspect() != null ? alias.getSuspect().getId() : null,
        alias.getAliasName(),
        alias.getAliasType(),
        alias.isPrimary(),
        alias.getCreatedAt()
    );
  }

  @Transactional
  public AliasResponse create(CreateAliasRequest request) {
    if (SecurityContextUtils.isComplianceUser()) {
      throw new AccessDeniedException("Compliance users have read-only access to the suspect registry");
    }
    log.debug("Creating alias for suspect ID: {}", request.suspectId());
    Suspect suspect = suspectRepo.findById(request.suspectId())
        .orElseThrow(() -> new ResourceNotFoundException("Suspect with ID " + request.suspectId() + " not found"));
    
    if (repo.findBySuspectIdAndAliasName(request.suspectId(), request.aliasName()).isPresent()) {
      throw new ResourceConflictException("Alias with name " + request.aliasName() + " already exists for this suspect");
    }
    
    Alias alias = new Alias();
    alias.setSuspect(suspect);
    alias.setAliasName(request.aliasName());
    alias.setAliasType(request.aliasType() != null ? request.aliasType() : DEFAULT_ALIAS_TYPE);
    alias.setPrimary(request.isPrimary() != null && request.isPrimary());
    Alias saved = repo.save(alias);
    log.info("Created alias with ID: {} for suspect ID: {}", saved.getId(), request.suspectId());
    return toResponse(saved);
  }

  public List<AliasResponse> findAll() {
    log.debug("Retrieving all aliases");
    return repo.findAll().stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  public AliasResponse findById(Long aliasId) {
    log.debug("Retrieving alias with ID: {}", aliasId);
    
    Alias alias = repo.findById(aliasId)
        .orElseThrow(() -> new ResourceNotFoundException("Alias with ID " + aliasId + " not found"));
    
    return toResponse(alias);
  }

  public List<AliasResponse> findBySuspectId(Long suspectId) {
    log.debug("Retrieving aliases for suspect ID: {}", suspectId);
    suspectRepo.findById(suspectId)
        .orElseThrow(() -> new ResourceNotFoundException("Suspect with ID " + suspectId + " not found"));
    return repo.findBySuspect_Id(suspectId).stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public AliasResponse updateById(Long aliasId, PatchAliasRequest request) {
    if (SecurityContextUtils.isComplianceUser()) {
      throw new AccessDeniedException("Compliance users have read-only access to the suspect registry");
    }
    log.debug("Patching alias with ID: {}", aliasId);
    Alias alias = repo.findById(aliasId)
        .orElseThrow(() -> new ResourceNotFoundException("Alias with ID " + aliasId + " not found"));
    boolean updated = false;
    if (request.suspectId() != null) {
      Suspect suspect = suspectRepo.findById(request.suspectId())
          .orElseThrow(() -> new ResourceNotFoundException("Suspect with ID " + request.suspectId() + " not found"));
      alias.setSuspect(suspect);
      updated = true;
    }
    if (request.aliasName() != null) { alias.setAliasName(request.aliasName()); updated = true; }
    if (request.aliasType() != null) { alias.setAliasType(request.aliasType()); updated = true; }
    if (request.isPrimary() != null) { alias.setPrimary(request.isPrimary()); updated = true; }
    if (!updated) {
      log.warn("No fields to update for alias with ID: {}", aliasId);
      return toResponse(alias);
    }
    
    // Check for uniqueness if suspectId or aliasName is being updated
    Long checkSuspectId = alias.getSuspect() != null ? alias.getSuspect().getId() : null;
    String checkAliasName = alias.getAliasName();
    
    if (checkSuspectId != null && checkAliasName != null) {
      repo.findBySuspectIdAndAliasName(checkSuspectId, checkAliasName).ifPresent(existing -> {
        if (existing.getId() != aliasId) {
          throw new ResourceConflictException("Alias with name " + checkAliasName + " already exists for this suspect");
        }
      });
    }
    
    Alias saved = repo.save(alias);
    log.info("Updated alias with ID: {}", saved.getId());
    return toResponse(saved);
  }

  @Transactional
  public void deleteById(Long aliasId) {
    if (SecurityContextUtils.isComplianceUser()) {
      throw new AccessDeniedException("Compliance users have read-only access to the suspect registry");
    }
    log.debug("Deleting alias with ID: {}", aliasId);
    if (!repo.existsById(aliasId)) {
      throw new ResourceNotFoundException("Alias with ID " + aliasId + " not found");
    }
    repo.deleteById(aliasId);
    log.info("Deleted alias with ID: {}", aliasId);
  }
}
