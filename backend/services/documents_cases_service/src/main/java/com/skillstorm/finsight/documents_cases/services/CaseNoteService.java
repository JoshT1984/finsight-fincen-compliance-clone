package com.skillstorm.finsight.documents_cases.services;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.finsight.documents_cases.dtos.request.CreateCaseNoteRequest;
import com.skillstorm.finsight.documents_cases.dtos.request.UpdateCaseNoteRequest;
import com.skillstorm.finsight.documents_cases.dtos.response.CaseNoteResponse;
import com.skillstorm.finsight.documents_cases.exceptions.ResourceNotFoundException;
import com.skillstorm.finsight.documents_cases.models.CaseNote;
import com.skillstorm.finsight.documents_cases.repositories.CaseFileRepository;
import com.skillstorm.finsight.documents_cases.repositories.CaseNoteRepository;
import com.skillstorm.finsight.documents_cases.repositories.DocumentRepository;
import com.skillstorm.finsight.documents_cases.utils.SecurityContextUtils;

@Service
public class CaseNoteService {
	
    private static final Logger log = LoggerFactory.getLogger(CaseNoteService.class);
    
    private final CaseNoteRepository repo;
    private final CaseFileRepository caseFileRepo;
    private final DocumentRepository documentRepo;
    private final AuditEventService auditEventService;

    public CaseNoteService(CaseNoteRepository repo, CaseFileRepository caseFileRepo, DocumentRepository documentRepo, AuditEventService auditEventService) {
    	this.repo = repo;
    	this.caseFileRepo = caseFileRepo;
    	this.documentRepo = documentRepo;
    	this.auditEventService = auditEventService;
    }
    
    private CaseNoteResponse toResponse(CaseNote caseNote) {
    	return new CaseNoteResponse(
    			caseNote.getNoteId(),
    			caseNote.getCaseId(),
    			caseNote.getAuthorUserId(),
    			caseNote.getNoteText(),
    			caseNote.getCreatedAt()
    	);
    }
    
    @Transactional
    public CaseNoteResponse create(CreateCaseNoteRequest dto) {
    	log.debug("Creating case note for case ID: {}", dto.caseId());
    	enforceCaseNoteAccess(dto.caseId());
    	CaseNote caseNote = new CaseNote();
    	caseNote.setCaseId(dto.caseId());
    	caseNote.setAuthorUserId(dto.authorUserId());
    	caseNote.setNoteText(dto.noteText());
    	caseNote.setCreatedAt(Instant.now());
    	
    	CaseNote saved = repo.save(caseNote);
    	log.info("Created case note with ID: {} for case ID: {}", saved.getNoteId(), saved.getCaseId());
    	
    	// Create audit event
    	auditEventService.auditCreate("CASE_NOTE", String.valueOf(saved.getNoteId()), saved);
    	
    	return toResponse(saved);
    }
    
    public List<CaseNoteResponse> findAll() {
    	log.debug("Retrieving all case notes");
    	if (SecurityContextUtils.isLawEnforcement()) {
    		Set<Long> visibleCaseIds = visibleToLawEnforcementCaseIds();
    		return repo.findByCaseIdIn(List.copyOf(visibleCaseIds)).stream()
    				.map(this::toResponse)
    				.collect(Collectors.toList());
    	}
    	if (SecurityContextUtils.isComplianceUser()) {
    		Set<Long> visibleCaseIds = visibleToComplianceUserCaseIds();
    		if (visibleCaseIds.isEmpty()) return List.of();
    		return repo.findByCaseIdIn(List.copyOf(visibleCaseIds)).stream()
    				.map(this::toResponse)
    				.collect(Collectors.toList());
    	}
    	if (SecurityContextUtils.isAnalyst()) {
    		return repo.findAll().stream()
    				.map(this::toResponse)
    				.collect(Collectors.toList());
    	}
    	return List.of();
    }

    public List<CaseNoteResponse> findByCaseId(Long caseId) {
    	log.debug("Retrieving case notes for case ID: {}", caseId);
    	enforceCaseNoteAccess(caseId);
    	return repo.findByCaseIdOrderByCreatedAtAsc(caseId).stream()
    			.map(this::toResponse)
    			.collect(Collectors.toList());
    }

    public CaseNoteResponse findById(Long noteId) {
    	log.debug("Retrieving case note with ID: {}", noteId);
    	CaseNote caseNote = repo.findById(noteId)
    			.orElseThrow(() -> new ResourceNotFoundException("Case note with ID " + noteId + " not found"));
    	enforceCaseNoteAccess(caseNote.getCaseId());
    	return toResponse(caseNote);
    }

    private void enforceCaseNoteAccess(Long caseId) {
    	if (SecurityContextUtils.isLawEnforcement() && !visibleToLawEnforcementCaseIds().contains(caseId)) {
    		throw new ResourceNotFoundException("Case with ID " + caseId + " not found");
    	}
    	if (SecurityContextUtils.isComplianceUser() && !visibleToComplianceUserCaseIds().contains(caseId)) {
    		throw new ResourceNotFoundException("Case with ID " + caseId + " not found");
    	}
    }

    private Set<Long> visibleToLawEnforcementCaseIds() {
    	return caseFileRepo.findVisibleToLawEnforcement().stream()
    			.map(c -> c.getCaseId())
    			.collect(Collectors.toSet());
    }

    private Set<Long> visibleToComplianceUserCaseIds() {
    	String userId = SecurityContextUtils.getCurrentUserId().map(UUID::toString).orElse(null);
    	if (userId == null) return Set.of();
    	return documentRepo.findDistinctCaseIdsByUploadedByUserId(userId).stream()
    			.collect(Collectors.toSet());
    }
    
    @Transactional
    public CaseNoteResponse updateById(Long noteId, UpdateCaseNoteRequest request) {
    	log.debug("Updating case note with ID: {}", noteId);
    	log.debug("Update request - noteText: {}", request.noteText());
    	
    	CaseNote caseNote = repo.findById(noteId)
    			.orElseThrow(() -> new ResourceNotFoundException("Case note with ID " + noteId + " not found"));
    	enforceCaseNoteAccess(caseNote.getCaseId());
    	// Create a copy of the old case note for audit comparison
    	CaseNote oldCaseNote = new CaseNote();
    	oldCaseNote.setNoteId(caseNote.getNoteId());
    	oldCaseNote.setCaseId(caseNote.getCaseId());
    	oldCaseNote.setAuthorUserId(caseNote.getAuthorUserId());
    	oldCaseNote.setNoteText(caseNote.getNoteText());
    	oldCaseNote.setCreatedAt(caseNote.getCreatedAt());
    	
    	boolean updated = false;
    	
    	if (request.noteText() != null) {
    		log.debug("Updating noteText from {} to {}", caseNote.getNoteText(), request.noteText());
    		caseNote.setNoteText(request.noteText());
    		updated = true;
    	}
    	
    	if (!updated) {
    		log.warn("No fields to update for case note with ID: {}", noteId);
    		return toResponse(caseNote);
    	}
    	
    	CaseNote saved = repo.save(caseNote);
    	log.info("Updated case note with ID: {} for case ID: {}", saved.getNoteId(), saved.getCaseId());
    	
    	// Create audit event for update
    	auditEventService.auditUpdate("CASE_NOTE", String.valueOf(saved.getNoteId()), oldCaseNote, saved);
    	
    	return toResponse(saved);
    }
    
    @Transactional
    public void deleteById(Long noteId) {
    	log.debug("Deleting case note with ID: {}", noteId);
    	
    	CaseNote caseNote = repo.findById(noteId)
    			.orElseThrow(() -> new ResourceNotFoundException("Case note with ID " + noteId + " not found"));
    	enforceCaseNoteAccess(caseNote.getCaseId());
    	// Create audit event before deletion
    	auditEventService.auditDelete("CASE_NOTE", String.valueOf(noteId), caseNote);
    	
    	repo.deleteById(noteId);
    	log.info("Deleted case note with ID: {}", noteId);
    }

}
