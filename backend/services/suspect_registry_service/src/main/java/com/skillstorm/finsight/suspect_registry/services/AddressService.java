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
import com.skillstorm.finsight.suspect_registry.exceptions.ResourceNotFoundException;
import com.skillstorm.finsight.suspect_registry.models.Address;
import com.skillstorm.finsight.suspect_registry.repositories.AddressRepository;

@Service
public class AddressService {
  
  private static final Logger log = LoggerFactory.getLogger(AddressService.class);
  
  private final AddressRepository repo;

  public AddressService(AddressRepository repo) {
    this.repo = repo;
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
        address.getAddressHash(),
        address.getCreatedAt()
    );
  }

  @Transactional
  public AddressResponse create(CreateAddressRequest request) {
    log.debug("Creating address");
    Address address = new Address();
    address.setLine1(request.line1());
    address.setLine2(request.line2());
    address.setCity(request.city());
    address.setState(request.state());
    address.setPostalCode(request.postalCode());
    address.setCountry(request.country());
    address.setAddressHash(request.addressHash());
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

  @Transactional
  public AddressResponse updateById(Long addressId, PatchAddressRequest request) {
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
    if (request.addressHash() != null) { address.setAddressHash(request.addressHash()); updated = true; }
    if (!updated) {
      log.warn("No fields to update for address with ID: {}", addressId);
      return toResponse(address);
    }
    Address saved = repo.save(address);
    log.info("Updated address with ID: {}", saved.getId());
    return toResponse(saved);
  }

  @Transactional
  public void deleteById(Long addressId) {
    log.debug("Deleting address with ID: {}", addressId);
    if (!repo.existsById(addressId)) {
      throw new ResourceNotFoundException("Address with ID " + addressId + " not found");
    }
    repo.deleteById(addressId);
    log.info("Deleted address with ID: {}", addressId);
  }
}
