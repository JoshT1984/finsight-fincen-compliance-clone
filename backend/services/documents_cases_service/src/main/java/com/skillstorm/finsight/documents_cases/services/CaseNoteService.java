package com.skillstorm.finsight.documents_cases.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.finsight.documents_cases.dtos.request.CreateCaseNoteRequest;
import com.skillstorm.finsight.documents_cases.dtos.request.UpdateCaseNoteRequest;
import com.skillstorm.finsight.documents_cases.dtos.response.CaseNoteResponse;
import com.skillstorm.finsight.documents_cases.exceptions.ResourceNotFoundException;
import com.skillstorm.finsight.documents_cases.models.CaseNote;
import com.skillstorm.finsight.documents_cases.repositories.CaseNoteRepository;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CaseNoteService {
	
    private static final Logger log = LoggerFactory.getLogger(CaseNoteService.class);
    
    private final CaseNoteRepository repo;
    private final AuditEventService auditEventService;
    
    public CaseNoteService(CaseNoteRepository repo, AuditEventService auditEventService) {
    	this.repo = repo;
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
    	return repo.findAll().stream()
    			.map(this::toResponse)
    			.collect(Collectors.toList());
    }
    
    public List<CaseNoteResponse> findByCaseId(Long caseId) {
    	log.debug("Retrieving case notes for case ID: {}", caseId);
    	return repo.findByCaseIdOrderByCreatedAtAsc(caseId).stream()
    			.map(this::toResponse)
    			.collect(Collectors.toList());
    }
    
    public CaseNoteResponse findById(Long noteId) {
    	log.debug("Retrieving case note with ID: {}", noteId);
    	
    	CaseNote caseNote = repo.findById(noteId)
    			.orElseThrow(() -> new ResourceNotFoundException("Case note with ID " + noteId + " not found"));
    	
    	return toResponse(caseNote);
    }
    
    @Transactional
    public CaseNoteResponse updateById(Long noteId, UpdateCaseNoteRequest request) {
    	log.debug("Updating case note with ID: {}", noteId);
    	log.debug("Update request - noteText: {}", request.noteText());
    	
    	CaseNote caseNote = repo.findById(noteId)
    			.orElseThrow(() -> new ResourceNotFoundException("Case note with ID " + noteId + " not found"));
    	
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
    	
    	// Create audit event before deletion
    	auditEventService.auditDelete("CASE_NOTE", String.valueOf(noteId), caseNote);
    	
    	repo.deleteById(noteId);
    	log.info("Deleted case note with ID: {}", noteId);
    }

}
