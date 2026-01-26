package com.skillstorm.finsight.documents_cases.controllers;

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

import com.skillstorm.finsight.documents_cases.dtos.request.CreateCaseNoteRequest;
import com.skillstorm.finsight.documents_cases.dtos.request.UpdateCaseNoteRequest;
import com.skillstorm.finsight.documents_cases.dtos.response.CaseNoteResponse;
import com.skillstorm.finsight.documents_cases.services.CaseNoteService;

@RestController
@RequestMapping("/api/case-notes")
public class CaseNoteController {
	
	private final CaseNoteService service;
	
	public CaseNoteController(CaseNoteService service) {
		this.service = service;
	}
	
	@PostMapping
	public ResponseEntity<CaseNoteResponse> create(@RequestBody CreateCaseNoteRequest request) {
		CaseNoteResponse response = service.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	@GetMapping
	public ResponseEntity<List<CaseNoteResponse>> getAll() {
		List<CaseNoteResponse> notes = service.findAll();
		return ResponseEntity.ok(notes);
	}
	
	@GetMapping("/case/{caseId}")
	public ResponseEntity<List<CaseNoteResponse>> getByCaseId(@PathVariable Long caseId) {
		List<CaseNoteResponse> notes = service.findByCaseId(caseId);
		return ResponseEntity.ok(notes);
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<CaseNoteResponse> getById(@PathVariable Long id) {
		CaseNoteResponse response = service.findById(id);
		return ResponseEntity.ok(response);
	}
	
	@PatchMapping("/{id}")
	public ResponseEntity<CaseNoteResponse> update(@PathVariable Long id, @RequestBody UpdateCaseNoteRequest request) {
		CaseNoteResponse response = service.updateById(id, request);
		return ResponseEntity.ok(response);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.deleteById(id);
		return ResponseEntity.noContent().build();
	}
}
