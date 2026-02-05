package com.skillstorm.finsight.suspect_registry.services;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.finsight.suspect_registry.dtos.request.CreateAddressRequest;
import com.skillstorm.finsight.suspect_registry.dtos.request.PatchAddressRequest;
import com.skillstorm.finsight.suspect_registry.dtos.response.AddressResponse;
import com.skillstorm.finsight.suspect_registry.exceptions.ResourceConflictException;
import com.skillstorm.finsight.suspect_registry.exceptions.ResourceNotFoundException;
import com.skillstorm.finsight.suspect_registry.models.Address;
import com.skillstorm.finsight.suspect_registry.repositories.AddressRepository;
import com.skillstorm.finsight.suspect_registry.repositories.SuspectAddressRepository;
import com.skillstorm.finsight.suspect_registry.repositories.SuspectRepository;
import com.skillstorm.finsight.suspect_registry.util.SecurityContextUtils;

import org.springframework.security.access.AccessDeniedException;

@Service
public class AddressService {
  
  private static final Logger log = LoggerFactory.getLogger(AddressService.class);
  
  private final AddressRepository repo;
  private final SuspectAddressRepository suspectAddressRepo;
  private final SuspectRepository suspectRepo;

  public AddressService(AddressRepository repo, SuspectAddressRepository suspectAddressRepo,
      SuspectRepository suspectRepo) {
    this.repo = repo;
    this.suspectAddressRepo = suspectAddressRepo;
    this.suspectRepo = suspectRepo;
  }

  private AddressResponse toResponse(Address address) {
    return new AddressResponse(
        address.getId(),
        address.getLine1(),
        address.getLine2(),
        address.getCity(),
        address.getState(),
        address.getPostalCode(),
        address.getCountry(),
        address.getCreatedAt()
    );
  }

  @Transactional
  public AddressResponse create(CreateAddressRequest request) {
    if (SecurityContextUtils.isComplianceUser()) {
      throw new AccessDeniedException("Compliance users have read-only access to the suspect registry");
    }
    log.debug("Creating address");

    if (repo.findByAddressComponents(
        request.line1(),
        request.line2(),
        request.city(),
        request.state(),
        request.postalCode(),
        request.country()).isPresent()) {
      throw new ResourceConflictException("Addresses must be unique (line1, line2, city, state, postalCode, country).");
    }
    
    Address address = new Address();
    address.setLine1(request.line1());
    address.setLine2(request.line2());
    address.setCity(request.city());
    address.setState(request.state());
    address.setPostalCode(request.postalCode());
    address.setCountry(request.country());
    Address saved = repo.save(address);
    log.info("Created address with ID: {}", saved.getId());
    return toResponse(saved);
  }

  public List<AddressResponse> findAll() {
    log.debug("Retrieving all addresses");
    return repo.findAll().stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  public AddressResponse findById(Long addressId) {
    log.debug("Retrieving address with ID: {}", addressId);
    
    Address address = repo.findById(addressId)
        .orElseThrow(() -> new ResourceNotFoundException("Address with ID " + addressId + " not found"));
    
    return toResponse(address);
  }

  public List<AddressResponse> findBySuspectId(Long suspectId) {
    log.debug("Retrieving addresses for suspect ID: {}", suspectId);
    suspectRepo.findById(suspectId)
        .orElseThrow(() -> new ResourceNotFoundException("Suspect with ID " + suspectId + " not found"));
    return suspectAddressRepo.findBySuspectIdOrderByLinkedAtDesc(suspectId).stream()
        .map(sa -> sa.getAddress())
        .filter(a -> a != null)
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public AddressResponse updateById(Long addressId, PatchAddressRequest request) {
    if (SecurityContextUtils.isComplianceUser()) {
      throw new AccessDeniedException("Compliance users have read-only access to the suspect registry");
    }
    log.debug("Patching address with ID: {}", addressId);
    Address address = repo.findById(addressId)
        .orElseThrow(() -> new ResourceNotFoundException("Address with ID " + addressId + " not found"));
    boolean updated = false;
    if (request.line1() != null) { address.setLine1(request.line1()); updated = true; }
    if (request.line2() != null) { address.setLine2(request.line2()); updated = true; }
    if (request.city() != null) { address.setCity(request.city()); updated = true; }
    if (request.state() != null) { address.setState(request.state()); updated = true; }
    if (request.postalCode() != null) { address.setPostalCode(request.postalCode()); updated = true; }
    if (request.country() != null) { address.setCountry(request.country()); updated = true; }
    if (!updated) {
      log.warn("No fields to update for address with ID: {}", addressId);
      return toResponse(address);
    }
    
    String checkLine1 = address.getLine1();
    String checkLine2 = address.getLine2();
    String checkCity = address.getCity();
    String checkState = address.getState();
    String checkPostalCode = address.getPostalCode();
    String checkCountry = address.getCountry();
    
    repo.findByAddressComponents(checkLine1, checkLine2, checkCity, checkState, checkPostalCode, checkCountry)
        .ifPresent(existing -> {
          if (existing.getId() != addressId) {
            throw new ResourceConflictException("Addresses must be unique (line1, line2, city, state, postalCode, country).");
          }
        });
    
    Address saved = repo.save(address);
    log.info("Updated address with ID: {}", saved.getId());
    return toResponse(saved);
  }

  @Transactional
  public void deleteById(Long addressId) {
    if (SecurityContextUtils.isComplianceUser()) {
      throw new AccessDeniedException("Compliance users have read-only access to the suspect registry");
    }
    log.debug("Deleting address with ID: {}", addressId);
    if (!repo.existsById(addressId)) {
      throw new ResourceNotFoundException("Address with ID " + addressId + " not found");
    }
    repo.deleteById(addressId);
    log.info("Deleted address with ID: {}", addressId);
  }
}
