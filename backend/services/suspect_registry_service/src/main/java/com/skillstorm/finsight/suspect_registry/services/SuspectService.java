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
import com.skillstorm.finsight.suspect_registry.dtos.response.LinkedOrganizationResponse;
import com.skillstorm.finsight.suspect_registry.dtos.response.SuspectResponse;
import com.skillstorm.finsight.suspect_registry.emitters.SuspectRegistryEventEmitter;
import com.skillstorm.finsight.suspect_registry.exceptions.ResourceConflictException;
import com.skillstorm.finsight.suspect_registry.exceptions.ResourceNotFoundException;
import com.skillstorm.finsight.suspect_registry.loggers.SuspectRegistryEventLog;
import com.skillstorm.finsight.suspect_registry.models.Address;
import com.skillstorm.finsight.suspect_registry.models.AddressType;
import com.skillstorm.finsight.suspect_registry.models.Alias;
import com.skillstorm.finsight.suspect_registry.models.AliasType;
import com.skillstorm.finsight.suspect_registry.models.Organization;
import com.skillstorm.finsight.suspect_registry.models.RiskLevel;
import com.skillstorm.finsight.suspect_registry.models.Suspect;
import com.skillstorm.finsight.suspect_registry.models.SuspectAddress;
import com.skillstorm.finsight.suspect_registry.models.SuspectAddressId;
import com.skillstorm.finsight.suspect_registry.models.SuspectOrganization;
import com.skillstorm.finsight.suspect_registry.models.SuspectOrganizationId;
import com.skillstorm.finsight.suspect_registry.repositories.AddressRepository;
import com.skillstorm.finsight.suspect_registry.repositories.AliasRepository;
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
  private final AliasRepository aliasRepo;
  private final AddressService addressService;
  private final SuspectRegistryEventEmitter eventEmitter;

  public SuspectService(SuspectRepository repo, SuspectOrganizationRepository suspectOrgRepo,
      SuspectAddressRepository suspectAddressRepo, OrganizationRepository orgRepo,
      AddressRepository addressRepo, AliasRepository aliasRepo, AddressService addressService,
      SuspectRegistryEventEmitter eventEmitter) {
    this.repo = repo;
    this.suspectOrgRepo = suspectOrgRepo;
    this.suspectAddressRepo = suspectAddressRepo;
    this.orgRepo = orgRepo;
    this.addressRepo = addressRepo;
    this.aliasRepo = aliasRepo;
    this.addressService = addressService;
    this.eventEmitter = eventEmitter;
  }

  private SuspectResponse toResponse(Suspect suspect) {
    return new SuspectResponse(
        suspect.getId(),
        suspect.getPrimaryName(),
        suspect.getDob(),
        suspect.getSsn(),
        suspect.getRiskLevel(),
        suspect.getCreatedAt(),
        suspect.getUpdatedAt());
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
    eventEmitter.emit(
        SuspectRegistryEventLog.suspectCreated(
            String.valueOf(saved.getId()),
            "USER",
            java.util.Map.of(
                "primaryName", saved.getPrimaryName(),
                "dob", saved.getDob(),
                "ssn", saved.getSsn(),
                "riskLevel", saved.getRiskLevel(),
                "actorUserId", SecurityContextUtils.getCurrentUserId().orElse(null))));
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

  /**
   * Finds a suspect by SSN (for compliance-event matching). Normalizes and hashes
   * SSN, then looks up by hash.
   * Returns empty if SSN is invalid or no suspect has that SSN.
   */
  @Transactional(readOnly = true)
  public java.util.Optional<Long> findSuspectIdBySsn(String ssn) {
    if (ssn == null || ssn.isBlank())
      return java.util.Optional.empty();
    try {
      SsnHashUtil.validateSsn(ssn);
    } catch (IllegalArgumentException e) {
      log.debug("Invalid SSN for lookup: {}", e.getMessage());
      return java.util.Optional.empty();
    }
    String hash = SsnHashUtil.hash(ssn);
    if (hash == null)
      return java.util.Optional.empty();
    return repo.findBySsnHash(hash).map(Suspect::getId);
  }

  /**
   * Finds a suspect by SSN, or creates one with the given name and SSN if not
   * found (for CTR/SAR upload flow).
   * Intended for server-to-server calls from compliance-event-service. Creates
   * with default risk level.
   */
  /**
   * Finds a suspect by SSN, or creates one with the given name and SSN if not
   * found (for CTR/SAR upload flow).
   * When an existing suspect is found by SSN and the form name differs from the
   * suspect's primary name,
   * the form name is added as an alias (AKA) so analysts can match suspects
   * across documents.
   */
  @Transactional
  public long findOrCreateBySsnAndName(String ssn, String primaryName) {
    if (ssn == null || ssn.isBlank()) {
      throw new IllegalArgumentException("SSN is required for find-or-create");
    }
    SsnHashUtil.validateSsn(ssn);
    String hash = SsnHashUtil.hash(ssn);
    java.util.Optional<Suspect> existing = repo.findBySsnHash(hash);
    if (existing.isPresent()) {
      Suspect s = existing.get();
      long id = s.getId();
      if (primaryName != null && !primaryName.isBlank()) {
        String formName = primaryName.trim();
        String currentPrimary = s.getPrimaryName() != null ? s.getPrimaryName().trim() : "";
        if (!formName.equalsIgnoreCase(currentPrimary)
            && aliasRepo.findBySuspectIdAndAliasName(id, formName).isEmpty()) {
          Alias alias = new Alias();
          alias.setSuspect(s);
          alias.setAliasName(formName.length() > 256 ? formName.substring(0, 256) : formName);
          alias.setAliasType(AliasType.AKA);
          alias.setPrimary(false);
          aliasRepo.save(alias);
          log.info("Added alias '{}' for suspect ID: {} (name from CTR/SAR differed from primary)", formName, id);
        }
      }
      return id;
    }
    String name = (primaryName != null && !primaryName.isBlank()) ? primaryName : "Unknown";
    if (name.length() > 256)
      name = name.substring(0, 256);
    Suspect suspect = new Suspect();
    suspect.setPrimaryName(name);
    suspect.setSsn(ssn);
    suspect.setRiskLevel(DEFAULT_RISK_LEVEL);
    Suspect saved = repo.save(suspect);
    log.info("Created suspect with ID: {} from SSN/name (find-or-create)", saved.getId());
    eventEmitter.emit(
        SuspectRegistryEventLog.suspectCreated(
            String.valueOf(saved.getId()),
            "SYSTEM",
            java.util.Map.of(
                "primaryName", saved.getPrimaryName(),
                "riskLevel", saved.getRiskLevel())));
    return saved.getId();
  }

  public List<SuspectResponse> findByOrganizationId(Long orgId) {
    log.debug("Retrieving suspects by organization ID: {}", orgId);
    return repo.findByOrganizationsOrganizationId(orgId).stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<LinkedOrganizationResponse> findOrganizationsBySuspectId(Long suspectId) {
    log.debug("Retrieving organizations for suspect ID: {}", suspectId);
    repo.findById(suspectId)
        .orElseThrow(() -> new ResourceNotFoundException("Suspect with ID " + suspectId + " not found"));
    return suspectOrgRepo.findBySuspectIdOrderByLinkedAtDesc(suspectId).stream()
        .map(so -> {
          Organization o = so.getOrganization();
          return new LinkedOrganizationResponse(
              o.getId(),
              o.getName(),
              o.getType(),
              so.getRole(),
              so.getLinkedAt());
        })
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
    if (request.primaryName() != null) {
      suspect.setPrimaryName(request.primaryName());
      updated = true;
    }
    if (request.dob() != null) {
      suspect.setDob(request.dob());
      updated = true;
    }
    if (request.ssn() != null) {
      suspect.setSsn(request.ssn());
      updated = true;
    }
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
    eventEmitter.emit(
        SuspectRegistryEventLog.suspectUpdated(
            String.valueOf(saved.getId()),
            "USER",
            java.util.Map.of(
                "primaryName", saved.getPrimaryName(),
                "dob", saved.getDob(),
                "riskLevel", saved.getRiskLevel(),
                "actorUserId", SecurityContextUtils.getCurrentUserId().orElse(null))));
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

  /**
   * Ensures an address from a CTR/SAR form is linked to the suspect
   * (find-or-create address, then link).
   * Used by compliance-event-service when a CTR/SAR is uploaded with parsed
   * address data.
   */
  @Transactional
  public void ensureAddressLinked(Long suspectId, String line1, String line2, String city, String state,
      String postalCode, String country) {
    if (line1 == null || line1.isBlank() || city == null || city.isBlank() || country == null || country.isBlank()) {
      return;
    }
    Suspect suspect = repo.findById(suspectId)
        .orElseThrow(() -> new ResourceNotFoundException("Suspect with ID " + suspectId + " not found"));
    Address address = addressService.findOrCreateByComponents(line1, line2, city, state, postalCode, country);
    if (suspectAddressRepo.existsById(new SuspectAddressId(suspectId, address.getId()))) {
      return;
    }
    SuspectAddress link = new SuspectAddress(suspect, address, DEFAULT_ADDRESS_TYPE, true, Instant.now());
    suspectAddressRepo.save(link);
    log.info("Linked address from form to suspect {} (address ID: {})", suspectId, address.getId());
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
    Suspect suspect = repo.findById(suspectId).orElse(null);
    repo.deleteById(suspectId);
    log.info("Deleted suspect with ID: {}", suspectId);
    if (suspect != null) {
      eventEmitter.emit(
          SuspectRegistryEventLog.suspectDeleted(
              String.valueOf(suspectId),
              "USER",
              java.util.Map.of(
                  "primaryName", suspect.getPrimaryName(),
                  "dob", suspect.getDob(),
                  "riskLevel", suspect.getRiskLevel(),
                  "actorUserId", SecurityContextUtils.getCurrentUserId().orElse(null))));
    }
  }
}
