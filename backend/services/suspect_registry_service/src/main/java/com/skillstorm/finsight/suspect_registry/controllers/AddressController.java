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

import com.skillstorm.finsight.suspect_registry.dtos.request.CreateAddressRequest;
import com.skillstorm.finsight.suspect_registry.dtos.request.PatchAddressRequest;
import com.skillstorm.finsight.suspect_registry.dtos.response.AddressResponse;
import com.skillstorm.finsight.suspect_registry.services.AddressService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {
  
  private final AddressService service;

  public AddressController(AddressService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<AddressResponse> create(@Valid @RequestBody CreateAddressRequest request) {
    AddressResponse response = service.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  public ResponseEntity<List<AddressResponse>> getAll() {
    List<AddressResponse> addresses = service.findAll();
    return ResponseEntity.ok(addresses);
  }
  
  @GetMapping("/{id}")
  public ResponseEntity<AddressResponse> getById(@PathVariable Long id) {
    AddressResponse response = service.findById(id);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}")
  public ResponseEntity<AddressResponse> update(@PathVariable Long id, @Valid @RequestBody PatchAddressRequest request) {
    AddressResponse response = service.updateById(id, request);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
