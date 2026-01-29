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

import com.skillstorm.finsight.suspect_registry.dtos.request.CreateAliasRequest;
import com.skillstorm.finsight.suspect_registry.dtos.request.PatchAliasRequest;
import com.skillstorm.finsight.suspect_registry.dtos.response.AliasResponse;
import com.skillstorm.finsight.suspect_registry.services.AliasService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/aliases")
public class AliasController {
  
  private final AliasService service;
  
  public AliasController(AliasService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<AliasResponse> create(@Valid @RequestBody CreateAliasRequest request) {
    AliasResponse response = service.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping
  public ResponseEntity<List<AliasResponse>> getAll() {
    List<AliasResponse> aliases = service.findAll();
    return ResponseEntity.ok(aliases);
  }

  @GetMapping("/{id}")
  public ResponseEntity<AliasResponse> getById(@PathVariable Long id) {
    AliasResponse response = service.findById(id);
    return ResponseEntity.ok(response);
  }

  @PatchMapping("/{id}")
  public ResponseEntity<AliasResponse> update(@PathVariable Long id, @Valid @RequestBody PatchAliasRequest request) {
    AliasResponse response = service.updateById(id, request);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
