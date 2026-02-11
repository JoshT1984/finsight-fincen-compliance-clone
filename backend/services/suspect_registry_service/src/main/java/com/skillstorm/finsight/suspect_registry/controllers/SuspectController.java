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

import com.skillstorm.finsight.suspect_registry.dtos.request.CreateSuspectRequest;
import com.skillstorm.finsight.suspect_registry.dtos.request.FindOrCreateBySsnRequest;
import com.skillstorm.finsight.suspect_registry.dtos.request.LinkSuspectAddressRequest;
import com.skillstorm.finsight.suspect_registry.dtos.request.LinkSuspectOrganizationRequest;
import com.skillstorm.finsight.suspect_registry.dtos.request.PatchSuspectRequest;
import com.skillstorm.finsight.suspect_registry.dtos.response.LinkedAddressResponse;
import com.skillstorm.finsight.suspect_registry.dtos.response.AliasResponse;
import com.skillstorm.finsight.suspect_registry.dtos.response.LinkedOrganizationResponse;
import com.skillstorm.finsight.suspect_registry.dtos.response.SuspectResponse;
import com.skillstorm.finsight.suspect_registry.services.AddressService;
import com.skillstorm.finsight.suspect_registry.services.AliasService;
import com.skillstorm.finsight.suspect_registry.services.SuspectService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/suspects")
public class SuspectController {
  
  private final SuspectService service;
  private final AliasService aliasService;
  private final AddressService addressService;
  
  public SuspectController(SuspectService service, AliasService aliasService, AddressService addressService) {
    this.service = service;
    this.aliasService = aliasService;
    this.addressService = addressService;
  }

  @PostMapping
  public ResponseEntity<SuspectResponse> create(@Valid @RequestBody CreateSuspectRequest request) {
    SuspectResponse response = service.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  public ResponseEntity<List<SuspectResponse>> getAll() {
    List<SuspectResponse> suspects = service.findAll();
    return ResponseEntity.ok(suspects);
  }

  /**
   * Lookup suspect by SSN (for compliance-event service to match CTR/SAR to suspects).
   * SSN can be 9 digits or xxx-xx-xxxx. Returns 200 with { "suspectId": id } or 404.
   */
  @GetMapping("/by-ssn")
  public ResponseEntity<java.util.Map<String, Long>> getBySsn(
      @org.springframework.web.bind.annotation.RequestParam String ssn) {
    return service.findSuspectIdBySsn(ssn)
        .map(id -> ResponseEntity.ok(java.util.Map.of("suspectId", id)))
        .orElse(ResponseEntity.notFound().build());
  }

  /**
   * Find suspect by SSN, or create one with the given name if not found (for CTR/SAR upload integration).
   * Returns 200 with { "suspectId": id }.
   */
  @PostMapping("/find-or-create")
  public ResponseEntity<java.util.Map<String, Long>> findOrCreate(
      @Valid @RequestBody FindOrCreateBySsnRequest request) {
    long id = service.findOrCreateBySsnAndName(request.ssn(), request.primaryName());
    return ResponseEntity.ok(java.util.Map.of("suspectId", id));
  }

  @GetMapping("/by-organization/{orgId}")
  public ResponseEntity<List<SuspectResponse>> getByOrganization(@PathVariable Long orgId) {
    List<SuspectResponse> suspects = service.findByOrganizationId(orgId);
    return ResponseEntity.ok(suspects);
  }

  /** GET linked organizations (path distinct from POST to avoid 405). */
  @GetMapping("/{id}/linked-organizations")
  public ResponseEntity<List<LinkedOrganizationResponse>> getOrganizationsBySuspect(@PathVariable Long id) {
    List<LinkedOrganizationResponse> organizations = service.findOrganizationsBySuspectId(id);
    return ResponseEntity.ok(organizations);
  }

  @GetMapping("/{id}/aliases")
  public ResponseEntity<List<AliasResponse>> getAliasesBySuspect(@PathVariable Long id) {
    List<AliasResponse> aliases = aliasService.findBySuspectId(id);
    return ResponseEntity.ok(aliases);
  }

  @GetMapping("/{id}/addresses")
  public ResponseEntity<List<LinkedAddressResponse>> getAddressesBySuspect(@PathVariable Long id) {
    List<LinkedAddressResponse> addresses = addressService.findBySuspectId(id);
    return ResponseEntity.ok(addresses);
  }

  @GetMapping("/{id}")
  public ResponseEntity<SuspectResponse> getById(@PathVariable Long id) {
    SuspectResponse response = service.findById(id);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}")
  public ResponseEntity<SuspectResponse> update(@PathVariable Long id, @Valid @RequestBody PatchSuspectRequest request) {
    SuspectResponse response = service.updateById(id, request);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.deleteById(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/organizations")
  public ResponseEntity<SuspectResponse> linkSuspectToOrganization(@PathVariable Long id,
      @Valid @RequestBody LinkSuspectOrganizationRequest request) {
    SuspectResponse response = service.linkSuspectToOrganization(id, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/{id}/addresses")
  public ResponseEntity<SuspectResponse> linkAddressToSuspect(@PathVariable Long id,
      @Valid @RequestBody LinkSuspectAddressRequest request) {
    SuspectResponse response = service.linkAddressToSuspect(id, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
