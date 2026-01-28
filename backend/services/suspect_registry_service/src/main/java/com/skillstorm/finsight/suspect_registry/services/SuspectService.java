package com.skillstorm.finsight.suspect_registry.services;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.finsight.suspect_registry.dtos.request.CreateSuspectRequest;
import com.skillstorm.finsight.suspect_registry.dtos.request.PatchSuspectRequest;
import com.skillstorm.finsight.suspect_registry.dtos.response.SuspectResponse;
import com.skillstorm.finsight.suspect_registry.exceptions.ResourceNotFoundException;
import com.skillstorm.finsight.suspect_registry.models.Suspect;
import com.skillstorm.finsight.suspect_registry.repositories.SuspectRepository;

@Service
public class SuspectService {
  
  private static final Logger log = LoggerFactory.getLogger(SuspectService.class);
  private static final String DEFAULT_RISK_LEVEL = "UNKNOWN";
  private static final Set<String> VALID_RISK_LEVELS = Set.of("UNKNOWN", "LOW", "MEDIUM", "HIGH");
  
  private final SuspectRepository repo;

  public SuspectService(SuspectRepository repo) {
    this.repo = repo;
  }

  private SuspectResponse toResponse(Suspect suspect) {
    return new SuspectResponse(
        suspect.getId(),
        suspect.getPrimaryName(),
        suspect.getDob(),
        suspect.getSsnLast4(),
        suspect.getRiskLevel(),
        suspect.getCreatedAt(),
        suspect.getUpdatedAt()
    );
  }

  private void validateRiskLevel(String riskLevel) {
    if (riskLevel != null && !VALID_RISK_LEVELS.contains(riskLevel)) {
      throw new IllegalArgumentException("Risk level must be one of: UNKNOWN, LOW, MEDIUM, HIGH");
    }
  }

  @Transactional
  public SuspectResponse create(CreateSuspectRequest request) {
    log.debug("Creating suspect: {}", request.primaryName());
    String risk = request.riskLevel() != null && !request.riskLevel().isBlank()
        ? request.riskLevel() : DEFAULT_RISK_LEVEL;
    validateRiskLevel(risk);
    Suspect suspect = new Suspect();
    suspect.setPrimaryName(request.primaryName());
    suspect.setDob(request.dob());
    suspect.setSsnLast4(request.ssnLast4());
    suspect.setRiskLevel(risk);
    Suspect saved = repo.save(suspect);
    log.info("Created suspect with ID: {}", saved.getId());
    return toResponse(saved);
  }

  public List<SuspectResponse> findAll() {
    log.debug("Retrieving all suspects");
    return repo.findAll().stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  public SuspectResponse findById(Long suspectId) {
    log.debug("Retrieving suspect with ID: {}", suspectId);
    
    Suspect suspect = repo.findById(suspectId)
        .orElseThrow(() -> new ResourceNotFoundException("Suspect with ID " + suspectId + " not found"));
    
    return toResponse(suspect);
  }

  @Transactional
  public SuspectResponse updateById(Long suspectId, PatchSuspectRequest request) {
    log.debug("Patching suspect with ID: {}", suspectId);
    Suspect suspect = repo.findById(suspectId)
        .orElseThrow(() -> new ResourceNotFoundException("Suspect with ID " + suspectId + " not found"));
    boolean updated = false;
    if (request.primaryName() != null) { suspect.setPrimaryName(request.primaryName()); updated = true; }
    if (request.dob() != null) { suspect.setDob(request.dob()); updated = true; }
    if (request.ssnLast4() != null) { suspect.setSsnLast4(request.ssnLast4()); updated = true; }
    if (request.riskLevel() != null) {
      validateRiskLevel(request.riskLevel());
      suspect.setRiskLevel(request.riskLevel());
      updated = true;
    }
    if (!updated) {
      log.warn("No fields to update for suspect with ID: {}", suspectId);
      return toResponse(suspect);
    }
    Suspect saved = repo.save(suspect);
    log.info("Updated suspect with ID: {}", saved.getId());
    return toResponse(saved);
  }

  @Transactional
  public void deleteById(Long suspectId) {
    log.debug("Deleting suspect with ID: {}", suspectId);
    if (!repo.existsById(suspectId)) {
      throw new ResourceNotFoundException("Suspect with ID " + suspectId + " not found");
    }
    repo.deleteById(suspectId);
    log.info("Deleted suspect with ID: {}", suspectId);
  }
}
