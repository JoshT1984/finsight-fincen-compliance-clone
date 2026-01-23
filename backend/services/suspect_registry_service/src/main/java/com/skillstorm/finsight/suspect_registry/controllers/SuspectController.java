package com.skillstorm.finsight.suspect_registry.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.skillstorm.finsight.suspect_registry.models.Suspect;
import com.skillstorm.finsight.suspect_registry.services.SuspectService;

@RestController
@RequestMapping("/suspect")
public class SuspectController {
  
  private final SuspectService suspectService;
  
  public SuspectController(SuspectService suspectService) {
    this.suspectService = suspectService;
  }

  @GetMapping
  public ResponseEntity<List<Suspect>> getAllSuspects() {
    try {
      List<Suspect> suspects = suspectService.getAllSuspects();
      return ResponseEntity.ok(suspects);
    } catch (Exception e) {
      return ResponseEntity.status(500).build();
    }
  }

  @PostMapping
  public ResponseEntity<Suspect> createSuspect(@RequestBody Suspect suspect) {
    try {
      Suspect createdSuspect = suspectService.createSuspect(suspect);
      return ResponseEntity.status(201).body(createdSuspect);
    } catch (Exception e) {
      return ResponseEntity.status(500).build();
    }
  }

}
