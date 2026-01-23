package com.skillstorm.finsight.suspect_registry.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.finsight.suspect_registry.models.Alias;
import com.skillstorm.finsight.suspect_registry.services.AliasService;

@RestController
@RequestMapping("/alias")
public class AliasController {
  
  private final AliasService aliasService;
  
  public AliasController(AliasService aliasService) {
    this.aliasService = aliasService;
  }

  @GetMapping
  public ResponseEntity<List<Alias>> getAllAliases() {
    try {
      List<Alias> aliases = aliasService.getAllAliases();
      return ResponseEntity.ok(aliases);
    } catch (Exception e) {
      return ResponseEntity.status(500).build();
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<Alias> getAliasById(@PathVariable long id) {
    try {
      Alias alias = aliasService.getAliasById(id);
      if (alias != null) {
        return ResponseEntity.ok(alias);
      } else {
        return ResponseEntity.notFound().build();
      }
    } catch (Exception e) {
      return ResponseEntity.status(500).build();
    }
  }

  @PostMapping
  public ResponseEntity<Alias> createAlias(@RequestBody Alias alias) {
    try {
      Alias createdAlias = aliasService.createAlias(alias);
      return ResponseEntity.status(201).body(createdAlias);
    } catch (Exception e) {
      return ResponseEntity.status(500).build();
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<Alias> updateAlias(@PathVariable long id, @RequestBody Alias alias) {
    try {
      Alias updatedAlias = aliasService.updateAlias(id, alias);
      if (updatedAlias != null) {
        return ResponseEntity.ok(updatedAlias);
      } else {
        return ResponseEntity.notFound().build();
      }
    } catch (Exception e) {
      return ResponseEntity.status(500).build();
    }
  }
}
