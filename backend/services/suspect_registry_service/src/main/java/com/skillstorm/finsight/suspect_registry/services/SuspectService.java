package com.skillstorm.finsight.suspect_registry.services;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.finsight.suspect_registry.dtos.request.CreateSuspectRequest;
import com.skillstorm.finsight.suspect_registry.dtos.request.LinkSuspectAddressRequest;
import com.skillstorm.finsight.suspect_registry.dtos.request.LinkSuspectOrganizationRequest;
import com.skillstorm.finsight.suspect_registry.dtos.request.PatchSuspectRequest;
import com.skillstorm.finsight.suspect_registry.dtos.response.SuspectResponse;
import com.skillstorm.finsight.suspect_registry.exceptions.ResourceConflictException;
import com.skillstorm.finsight.suspect_registry.exceptions.ResourceNotFoundException;
import com.skillstorm.finsight.suspect_registry.models.Address;
import com.skillstorm.finsight.suspect_registry.models.AddressType;
import com.skillstorm.finsight.suspect_registry.models.Organization;
import com.skillstorm.finsight.suspect_registry.models.RiskLevel;
import com.skillstorm.finsight.suspect_registry.models.Suspect;
import com.skillstorm.finsight.suspect_registry.models.SuspectAddress;
import com.skillstorm.finsight.suspect_registry.models.SuspectAddressId;
import com.skillstorm.finsight.suspect_registry.models.SuspectOrganization;
import com.skillstorm.finsight.suspect_registry.models.SuspectOrganizationId;
import com.skillstorm.finsight.suspect_registry.repositories.AddressRepository;
import com.skillstorm.finsight.suspect_registry.repositories.OrganizationRepository;
import com.skillstorm.finsight.suspect_registry.repositories.SuspectAddressRepository;
import com.skillstorm.finsight.suspect_registry.repositories.SuspectOrganizationRepository;
import com.skillstorm.finsight.suspect_registry.repositories.SuspectRepository;
import com.skillstorm.finsight.suspect_registry.util.SecurityContextUtils;
import com.skillstorm.finsight.suspect_registry.util.SsnHashUtil;

@Service
public class SuspectService {
  
  private static final Logger log = LoggerFactory.getLogger(SuspectService.class);
  private static final RiskLevel DEFAULT_RISK_LEVEL = RiskLevel.UNKNOWN;
  private static final AddressType DEFAULT_ADDRESS_TYPE = AddressType.UNKNOWN;
  
  private final SuspectRepository repo;
  private final SuspectOrganizationRepository suspectOrgRepo;
  private final SuspectAddressRepository suspectAddressRepo;
  private final OrganizationRepository orgRepo;
  private final AddressRepository addressRepo;

  public SuspectService(SuspectRepository repo, SuspectOrganizationRepository suspectOrgRepo,
      SuspectAddressRepository suspectAddressRepo, OrganizationRepository orgRepo,
      AddressRepository addressRepo) {
    this.repo = repo;
    this.suspectOrgRepo = suspectOrgRepo;
    this.suspectAddressRepo = suspectAddressRepo;
    this.orgRepo = orgRepo;
    this.addressRepo = addressRepo;
  }

  private SuspectResponse toResponse(Suspect suspect) {
    return new SuspectResponse(
        suspect.getId(),
        suspect.getPrimaryName(),
        suspect.getDob(),
        suspect.getSsn(),
        suspect.getRiskLevel(),
        suspect.getCreatedAt(),
        suspect.getUpdatedAt()
    );
  }

  @Transactional
  public SuspectResponse create(CreateSuspectRequest request) {
    if (SecurityContextUtils.isComplianceUser()) {
      throw new AccessDeniedException("Compliance users have read-only access to the suspect registry");
    }
    log.debug("Creating suspect: {}", request.primaryName());

    if (request.ssn() != null && !request.ssn().isBlank()) {
      SsnHashUtil.validateSsn(request.ssn());
      String ssnHash = SsnHashUtil.hash(request.ssn());
      if (ssnHash != null && repo.findBySsnHash(ssnHash).isPresent()) {
        throw new ResourceConflictException("Suspect with SSN already exists");
      }
    }
    
    RiskLevel risk = request.riskLevel() != null ? request.riskLevel() : DEFAULT_RISK_LEVEL;
    Suspect suspect = new Suspect();
    suspect.setPrimaryName(request.primaryName());
    suspect.setDob(request.dob());
    suspect.setSsn(request.ssn());
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

  public List<SuspectResponse> findByOrganizationId(Long orgId) {
    log.debug("Retrieving suspects by organization ID: {}", orgId);
    return repo.findByOrganizationsOrganizationId(orgId).stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public SuspectResponse updateById(Long suspectId, PatchSuspectRequest request) {
    if (SecurityContextUtils.isComplianceUser()) {
      throw new AccessDeniedException("Compliance users have read-only access to the suspect registry");
    }
    log.debug("Patching suspect with ID: {}", suspectId);
    Suspect suspect = repo.findById(suspectId)
        .orElseThrow(() -> new ResourceNotFoundException("Suspect with ID " + suspectId + " not found"));
    boolean updated = false;
    if (request.primaryName() != null) { suspect.setPrimaryName(request.primaryName()); updated = true; }
    if (request.dob() != null) { suspect.setDob(request.dob()); updated = true; }
    if (request.ssn() != null) { suspect.setSsn(request.ssn()); updated = true; }
    if (request.riskLevel() != null) {
      suspect.setRiskLevel(request.riskLevel());
      updated = true;
    }
    if (!updated) {
      log.warn("No fields to update for suspect with ID: {}", suspectId);
      return toResponse(suspect);
    }
    
    // Check for SSN uniqueness if SSN is being updated
    if (request.ssn() != null && !request.ssn().isBlank()) {
      SsnHashUtil.validateSsn(request.ssn());
      String ssnHash = SsnHashUtil.hash(request.ssn());
      if (ssnHash != null) {
        repo.findBySsnHash(ssnHash).ifPresent(existing -> {
          if (existing.getId() != suspectId) {
            throw new ResourceConflictException("Suspect with SSN already exists");
          }
        });
      }
    }
    
    Suspect saved = repo.save(suspect);
    log.info("Updated suspect with ID: {}", saved.getId());
    return toResponse(saved);
  }

  @Transactional
  public SuspectResponse linkSuspectToOrganization(Long suspectId, LinkSuspectOrganizationRequest request) {
    if (SecurityContextUtils.isComplianceUser()) {
      throw new AccessDeniedException("Compliance users have read-only access to the suspect registry");
    }
    log.debug("Linking suspect {} to organization {}", suspectId, request.orgId());
    Suspect suspect = repo.findById(suspectId)
        .orElseThrow(() -> new ResourceNotFoundException("Suspect with ID " + suspectId + " not found"));
    Organization org = orgRepo.findById(request.orgId())
        .orElseThrow(() -> new ResourceNotFoundException("Organization with ID " + request.orgId() + " not found"));
    if (suspectOrgRepo.existsById(new SuspectOrganizationId(suspectId, request.orgId()))) {
      throw new ResourceConflictException("Suspect is already linked to this organization");
    }
    SuspectOrganization link = new SuspectOrganization(suspect, org, request.role(), Instant.now());
    suspectOrgRepo.save(link);
    log.info("Linked suspect {} to organization {}", suspectId, request.orgId());
    return toResponse(repo.findById(suspectId).orElseThrow());
  }

  @Transactional
  public SuspectResponse linkAddressToSuspect(Long suspectId, LinkSuspectAddressRequest request) {
    if (SecurityContextUtils.isComplianceUser()) {
      throw new AccessDeniedException("Compliance users have read-only access to the suspect registry");
    }
    log.debug("Linking address {} to suspect {}", request.addressId(), suspectId);
    Suspect suspect = repo.findById(suspectId)
        .orElseThrow(() -> new ResourceNotFoundException("Suspect with ID " + suspectId + " not found"));
    Address address = addressRepo.findById(request.addressId())
        .orElseThrow(() -> new ResourceNotFoundException("Address with ID " + request.addressId() + " not found"));
    if (suspectAddressRepo.existsById(new SuspectAddressId(suspectId, request.addressId()))) {
      throw new ResourceConflictException("Address is already linked to this suspect");
    }
    AddressType addrType = request.addressType() != null ? request.addressType() : DEFAULT_ADDRESS_TYPE;
    boolean isCurrent = request.isCurrent() != null ? request.isCurrent() : true;
    SuspectAddress link = new SuspectAddress(suspect, address, addrType, isCurrent, Instant.now());
    suspectAddressRepo.save(link);
    log.info("Linked address {} to suspect {}", request.addressId(), suspectId);
    return toResponse(repo.findById(suspectId).orElseThrow());
  }

  @Transactional
  public void deleteById(Long suspectId) {
    if (SecurityContextUtils.isComplianceUser()) {
      throw new AccessDeniedException("Compliance users have read-only access to the suspect registry");
    }
    log.debug("Deleting suspect with ID: {}", suspectId);
    if (!repo.existsById(suspectId)) {
      throw new ResourceNotFoundException("Suspect with ID " + suspectId + " not found");
    }
    repo.deleteById(suspectId);
    log.info("Deleted suspect with ID: {}", suspectId);
  }
}
