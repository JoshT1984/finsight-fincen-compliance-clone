package com.skillstorm.finsight.documents_cases.controllers;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.skillstorm.finsight.documents_cases.dtos.request.CreateCaseFileRequest;
import com.skillstorm.finsight.documents_cases.dtos.request.ReferCaseRequest;
import com.skillstorm.finsight.documents_cases.dtos.request.UpdateCaseFileRequest;
import com.skillstorm.finsight.documents_cases.dtos.response.CaseFileResponse;
import com.skillstorm.finsight.documents_cases.services.CaseFileService;
import com.skillstorm.finsight.documents_cases.services.DocumentService;

@RestController
@RequestMapping("/api/cases")
public class CaseFileController {

	private final CaseFileService service;
	private final DocumentService documentService;

	public CaseFileController(CaseFileService service, DocumentService documentService) {
		this.service = service;
		this.documentService = documentService;
	}
	
	@PostMapping
	public ResponseEntity<CaseFileResponse> create(@RequestBody CreateCaseFileRequest request) {
		CaseFileResponse response = service.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	
	@GetMapping
	public ResponseEntity<List<CaseFileResponse>> getAll() {
		List<CaseFileResponse> cases = service.findAll();
		return ResponseEntity.ok(cases);
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<CaseFileResponse> getById(@PathVariable Long id) {
		CaseFileResponse response = service.findById(id);
		return ResponseEntity.ok(response);
	}
	
	@PatchMapping("/{id}")
	public ResponseEntity<CaseFileResponse> update(@PathVariable Long id, @RequestBody UpdateCaseFileRequest request) {
		CaseFileResponse response = service.updateById(id, request);
		return ResponseEntity.ok(response);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.deleteById(id);
		return ResponseEntity.noContent().build();
	}
	
	@PostMapping("/{id}/refer")
	public ResponseEntity<CaseFileResponse> refer(@PathVariable Long id, @RequestBody ReferCaseRequest request) {
		CaseFileResponse response = service.referById(id, request);
		return ResponseEntity.ok(response);
	}
	
	@PostMapping("/{id}/close")
	public ResponseEntity<CaseFileResponse> close(@PathVariable Long id) {
		CaseFileResponse response = service.closeById(id);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{id}/documents/zip")
	public ResponseEntity<StreamingResponseBody> downloadDocumentsZip(@PathVariable Long id) {
		String filename = "case-" + id + "-documents.zip";
		StreamingResponseBody stream = out -> documentService.writeCaseDocumentsZip(id, out);
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType("application/zip"))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
				.body(stream);
	}
}
