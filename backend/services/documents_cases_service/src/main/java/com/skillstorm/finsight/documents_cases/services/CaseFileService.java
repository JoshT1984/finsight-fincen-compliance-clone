package com.skillstorm.finsight.documents_cases.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.skillstorm.finsight.documents_cases.dtos.request.CreateCaseFileRequest;
import com.skillstorm.finsight.documents_cases.dtos.request.ReferCaseRequest;
import com.skillstorm.finsight.documents_cases.dtos.request.UpdateCaseFileRequest;
import com.skillstorm.finsight.documents_cases.dtos.response.CaseFileResponse;
import com.skillstorm.finsight.documents_cases.exceptions.ResourceConflictException;
import com.skillstorm.finsight.documents_cases.exceptions.ResourceNotFoundException;
import com.skillstorm.finsight.documents_cases.models.CaseFile;
import com.skillstorm.finsight.documents_cases.models.CaseStatus;
import com.skillstorm.finsight.documents_cases.repositories.CaseFileRepository;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CaseFileService {
	
    private static final Logger log = LoggerFactory.getLogger(CaseFileService.class);
    
    private final CaseFileRepository repo;
    
    public CaseFileService(CaseFileRepository repo) {
    	this.repo = repo;
    }
    
    private CaseFileResponse toResponse(CaseFile caseFile) {
    	return new CaseFileResponse(
    			caseFile.getCaseId(),
    			caseFile.getSarId(),
    			caseFile.getStatus(),
    			caseFile.getCreatedAt(),
    			caseFile.getReferredAt(),
    			caseFile.getClosedAt(),
    			caseFile.getReferredToAgency()
    	);
    }
    
    @Transactional
    public CaseFileResponse create(CreateCaseFileRequest dto) {
    	log.debug("Creating case file for SAR ID: {}", dto.sarId());
    	
    	if (repo.findBySarId(dto.sarId()).isPresent()) {
    		throw new ResourceConflictException("Case file with SAR ID " + dto.sarId() + " already exists");
    	}
    	
    	Instant now = Instant.now();
    	CaseFile caseFile = new CaseFile();
    	caseFile.setSarId(dto.sarId());
    	caseFile.setStatus(dto.status() != null ? dto.status() : CaseStatus.OPEN);
    	caseFile.setCreatedAt(now);
    	caseFile.setReferredToAgency(dto.referredToAgency());
    	
    	// If referredToAgency is provided, also set referredAt timestamp
    	if (dto.referredToAgency() != null && !dto.referredToAgency().trim().isEmpty()) {
    		caseFile.setReferredAt(now);
    	}
    	
    	CaseFile saved = repo.save(caseFile);
    	log.info("Created case file with ID: {} for SAR ID: {}", saved.getCaseId(), saved.getSarId());
    	
    	return toResponse(saved);
    }
    
    public List<CaseFileResponse> findAll() {
    	log.debug("Retrieving all case files");
    	return repo.findAll().stream()
    			.map(this::toResponse)
    			.collect(Collectors.toList());
    }
    
    @Transactional
    public CaseFileResponse referById(Long caseId, ReferCaseRequest request) {
    	log.debug("Referring case file with ID: {} to agency: {}", caseId, request.referredToAgency());
    	
    	CaseFile caseFile = repo.findById(caseId)
    			.orElseThrow(() -> new ResourceNotFoundException("Case file with ID " + caseId + " not found"));
    	
    	if (request.referredToAgency() == null || request.referredToAgency().trim().isEmpty()) {
    		throw new IllegalArgumentException("Referred to agency is required");
    	}
    	
    	caseFile.setStatus(CaseStatus.REFERRED);
    	caseFile.setReferredToAgency(request.referredToAgency());
    	caseFile.setReferredAt(Instant.now());
    	
    	CaseFile saved = repo.save(caseFile);
    	log.info("Referred case file with ID: {} (SAR ID: {}) to agency: {}", 
    			saved.getCaseId(), saved.getSarId(), saved.getReferredToAgency());
    	
    	return toResponse(saved);
    }
    
    @Transactional
    public CaseFileResponse closeById(Long caseId) {
    	log.debug("Closing case file with ID: {}", caseId);
    	
    	CaseFile caseFile = repo.findById(caseId)
    			.orElseThrow(() -> new ResourceNotFoundException("Case file with ID " + caseId + " not found"));
    	
    	caseFile.setStatus(CaseStatus.CLOSED);
    	caseFile.setClosedAt(Instant.now());
    	
    	CaseFile saved = repo.save(caseFile);
    	log.info("Closed case file with ID: {} (SAR ID: {})", saved.getCaseId(), saved.getSarId());
    	
    	return toResponse(saved);
    }
    
    public CaseFileResponse findById(Long caseId) {
    	log.debug("Retrieving case file with ID: {}", caseId);
    	
    	CaseFile caseFile = repo.findById(caseId)
    			.orElseThrow(() -> new ResourceNotFoundException("Case file with ID " + caseId + " not found"));
    	
    	return toResponse(caseFile);
    }
    
    @Transactional
    public CaseFileResponse updateById(Long caseId, UpdateCaseFileRequest request) {
    	log.debug("Updating case file with ID: {}", caseId);
    	log.debug("Update request - sarId: {}, status: {}, referredToAgency: {}", 
    			request.sarId(), request.status(), request.referredToAgency());
    	
    	CaseFile caseFile = repo.findById(caseId)
    			.orElseThrow(() -> new ResourceNotFoundException("Case file with ID " + caseId + " not found"));
    	
    	boolean updated = false;
    	
    	if (request.sarId() != null) {

    		repo.findBySarId(request.sarId()).ifPresent(existing -> {
    			if (!existing.getCaseId().equals(caseId)) {
    				throw new ResourceConflictException("Case file with SAR ID " + request.sarId() + " already exists");
    			}
    		});
    		log.debug("Updating sarId from {} to {}", caseFile.getSarId(), request.sarId());
    		caseFile.setSarId(request.sarId());
    		updated = true;
    	}
    	
    	if (request.status() != null) {
    		log.debug("Updating status from {} to {}", caseFile.getStatus(), request.status());
    		caseFile.setStatus(request.status());
    		updated = true;
    	}
    	
    	if (request.referredToAgency() != null) {
    		log.debug("Updating referredToAgency from {} to {}", caseFile.getReferredToAgency(), request.referredToAgency());
    		caseFile.setReferredToAgency(request.referredToAgency());
    		updated = true;
    	}
    	
    	if (!updated) {
    		log.warn("No fields to update for case file with ID: {}", caseId);
    		return toResponse(caseFile);
    	}
    	
    	CaseFile saved = repo.save(caseFile);
    	log.info("Updated case file with ID: {} (SAR ID: {})", saved.getCaseId(), saved.getSarId());
    	
    	return toResponse(saved);
    }
    
    @Transactional
    public void deleteById(Long caseId) {
    	log.debug("Deleting case file with ID: {}", caseId);
    	
    	if (!repo.existsById(caseId)) {
    		throw new ResourceNotFoundException("Case file with ID " + caseId + " not found");
    	}
    	
    	repo.deleteById(caseId);
    	log.info("Deleted case file with ID: {}", caseId);
    }

}
