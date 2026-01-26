package com.skillstorm.finsight.documents_cases.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillstorm.finsight.documents_cases.dtos.response.AuditEventResponse;
import com.skillstorm.finsight.documents_cases.models.AuditEvent;
import com.skillstorm.finsight.documents_cases.repositories.AuditEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for querying audit events.
 * 
 * <p>Provides endpoints to retrieve audit history for entities, users, and actions.
 */
@RestController
@RequestMapping("/api/audit-events")
public class AuditEventController {
	
	private final AuditEventRepository repository;
	private final ObjectMapper objectMapper;
	
	public AuditEventController(AuditEventRepository repository, ObjectMapper objectMapper) {
		this.repository = repository;
		this.objectMapper = objectMapper;
	}
	
	/**
	 * Gets all audit events for a specific entity.
	 * 
	 * @param entityType The type of entity (e.g., "DOCUMENT", "CASE", "CASE_NOTE")
	 * @param entityId The ID of the entity
	 * @return List of audit events for the entity, ordered by creation date (newest first)
	 */
	@GetMapping("/entity/{entityType}/{entityId}")
	public ResponseEntity<List<AuditEventResponse>> getByEntity(
			@PathVariable String entityType,
			@PathVariable String entityId) {
		List<AuditEvent> events = repository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
		List<AuditEventResponse> responses = events.stream()
				.map(event -> AuditEventResponse.fromEntity(event, objectMapper))
				.collect(Collectors.toList());
		return ResponseEntity.ok(responses);
	}
	
	/**
	 * Gets all audit events performed by a specific user.
	 * 
	 * @param userId The user ID (UUID)
	 * @return List of audit events by the user
	 */
	@GetMapping("/user/{userId}")
	public ResponseEntity<List<AuditEventResponse>> getByUser(@PathVariable UUID userId) {
		List<AuditEvent> events = repository.findByActorUserId(userId);
		List<AuditEventResponse> responses = events.stream()
				.map(event -> AuditEventResponse.fromEntity(event, objectMapper))
				.collect(Collectors.toList());
		return ResponseEntity.ok(responses);
	}
	
	/**
	 * Gets all audit events for a specific action.
	 * 
	 * @param action The action name (e.g., "CREATE", "UPDATE", "DELETE", "REFER", "CLOSE", "UPLOAD")
	 * @return List of audit events for the action
	 */
	@GetMapping("/action/{action}")
	public ResponseEntity<List<AuditEventResponse>> getByAction(@PathVariable String action) {
		List<AuditEvent> events = repository.findByAction(action);
		List<AuditEventResponse> responses = events.stream()
				.map(event -> AuditEventResponse.fromEntity(event, objectMapper))
				.collect(Collectors.toList());
		return ResponseEntity.ok(responses);
	}
	
	/**
	 * Gets all audit events.
	 * 
	 * @return List of all audit events
	 */
	@GetMapping
	public ResponseEntity<List<AuditEventResponse>> getAll() {
		List<AuditEvent> events = repository.findAll();
		List<AuditEventResponse> responses = events.stream()
				.map(event -> AuditEventResponse.fromEntity(event, objectMapper))
				.collect(Collectors.toList());
		return ResponseEntity.ok(responses);
	}
}
