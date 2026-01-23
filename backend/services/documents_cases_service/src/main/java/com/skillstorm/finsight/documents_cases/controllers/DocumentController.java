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

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.skillstorm.finsight.documents_cases.dtos.request.CreateDocumentRequest;
import com.skillstorm.finsight.documents_cases.dtos.request.UpdateDocumentRequest;
import com.skillstorm.finsight.documents_cases.dtos.response.DocumentDownloadUrlResponse;
import com.skillstorm.finsight.documents_cases.dtos.response.DocumentResponse;
import com.skillstorm.finsight.documents_cases.models.DocumentType;
import com.skillstorm.finsight.documents_cases.services.DocumentService;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
	
	private final DocumentService service;
	
	public DocumentController(DocumentService service) {
		this.service = service;
	}
	
	@PostMapping
	public ResponseEntity<DocumentResponse> create(@RequestBody CreateDocumentRequest request) {
		DocumentResponse response = service.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
	@GetMapping
	public ResponseEntity<List<DocumentResponse>> getAll() {
		List<DocumentResponse> documents = service.findAll();
		return ResponseEntity.ok(documents);
	}
	
	@GetMapping("/{id}")
	public ResponseEntity<DocumentResponse> getById(@PathVariable Long id) {
		DocumentResponse response = service.findById(id);
		return ResponseEntity.ok(response);
	}
	
	@GetMapping("/ctr/{ctrId}")
	public ResponseEntity<List<DocumentResponse>> getByCtrId(@PathVariable Long ctrId) {
		List<DocumentResponse> documents = service.findByCtrId(ctrId);
		return ResponseEntity.ok(documents);
	}
	
	@GetMapping("/sar/{sarId}")
	public ResponseEntity<List<DocumentResponse>> getBySarId(@PathVariable Long sarId) {
		List<DocumentResponse> documents = service.findBySarId(sarId);
		return ResponseEntity.ok(documents);
	}
	
	@GetMapping("/case/{caseId}")
	public ResponseEntity<List<DocumentResponse>> getByCaseId(@PathVariable Long caseId) {
		List<DocumentResponse> documents = service.findByCaseId(caseId);
		return ResponseEntity.ok(documents);
	}
	
	@PatchMapping("/{id}")
	public ResponseEntity<DocumentResponse> update(@PathVariable Long id, @RequestBody UpdateDocumentRequest request) {
		DocumentResponse response = service.updateById(id, request);
		return ResponseEntity.ok(response);
	}
	
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		service.deleteById(id);
		return ResponseEntity.noContent().build();
	}
	
	@GetMapping("/{id}/download-url")
	public ResponseEntity<DocumentDownloadUrlResponse> getDownloadUrl(
			@PathVariable Long id,
			@RequestParam(required = false) Integer expirationMinutes) {
		DocumentDownloadUrlResponse response = service.getDownloadUrl(id, expirationMinutes);
		return ResponseEntity.ok(response);
	}
	
	@PostMapping("/upload")
	public ResponseEntity<DocumentResponse> upload(
			@RequestParam("file") MultipartFile file,
			@RequestParam("documentType") String documentTypeStr,
			@RequestParam(required = false) Long ctrId,
			@RequestParam(required = false) Long sarId,
			@RequestParam(required = false) Long caseId) {
		try {
			DocumentType documentType = DocumentType.valueOf(documentTypeStr.toUpperCase());
			DocumentResponse response = service.upload(file, documentType, ctrId, sarId, caseId);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Invalid document type: " + documentTypeStr + ". Must be one of: CTR, SAR, CASE", e);
		} catch (java.io.IOException e) {
			throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
		}
	}
}
