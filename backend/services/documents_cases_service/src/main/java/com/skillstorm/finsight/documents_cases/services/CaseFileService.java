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

import com.skillstorm.finsight.documents_cases.dtos.request.CreateCaseFileRequest;
import com.skillstorm.finsight.documents_cases.dtos.request.ReferCaseRequest;
import com.skillstorm.finsight.documents_cases.dtos.request.UpdateCaseFileRequest;
import com.skillstorm.finsight.documents_cases.dtos.response.CaseFileResponse;
import com.skillstorm.finsight.documents_cases.exceptions.ResourceConflictException;
import com.skillstorm.finsight.documents_cases.exceptions.ResourceNotFoundException;
import com.skillstorm.finsight.documents_cases.models.CaseFile;
import com.skillstorm.finsight.documents_cases.models.CaseStatus;
import com.skillstorm.finsight.documents_cases.repositories.CaseFileRepository;
import com.skillstorm.finsight.documents_cases.repositories.DocumentRepository;
import com.skillstorm.finsight.documents_cases.utils.SecurityContextUtils;

@Service
public class CaseFileService {

	private static final Logger log = LoggerFactory.getLogger(CaseFileService.class);

	private final CaseFileRepository repo;
	private final DocumentRepository documentRepo;
	private final AuditEventService auditEventService;

	public CaseFileService(CaseFileRepository repo, DocumentRepository documentRepo,
			AuditEventService auditEventService) {
		this.repo = repo;
		this.documentRepo = documentRepo;
		this.auditEventService = auditEventService;
	}

	private CaseFileResponse toResponse(CaseFile caseFile) {
		return new CaseFileResponse(
				caseFile.getCaseId(),
				caseFile.getSarId(),
				caseFile.getCtrId(),
				caseFile.getStatus(),
				caseFile.getCreatedAt(),
				caseFile.getReferredAt(),
				caseFile.getClosedAt(),
				caseFile.getReferredToAgency());
	}

	@Transactional
	public CaseFileResponse create(CreateCaseFileRequest dto) {

		boolean hasSar = dto.sarId() != null;
		boolean hasCtr = dto.ctrId() != null;

		if (hasSar == hasCtr) {
			// both true or both false
			throw new IllegalArgumentException("Exactly one of sarId or ctrId must be provided");
		}

		if (hasSar && repo.findBySarId(dto.sarId()).isPresent()) {
			throw new ResourceConflictException("Case file with SAR ID " + dto.sarId() + " already exists");
		}

		if (hasCtr && repo.findByCtrId(dto.ctrId()).isPresent()) {
			throw new ResourceConflictException("Case file with CTR ID " + dto.ctrId() + " already exists");
		}

		Instant now = Instant.now();

		CaseFile caseFile = new CaseFile();
		caseFile.setSarId(dto.sarId());
		caseFile.setCtrId(dto.ctrId());
		caseFile.setStatus(dto.status() != null ? dto.status() : CaseStatus.OPEN);
		caseFile.setCreatedAt(now);
		caseFile.setReferredToAgency(dto.referredToAgency());

		if (dto.referredToAgency() != null && !dto.referredToAgency().trim().isEmpty()) {
			caseFile.setReferredAt(now);
		}

		CaseFile saved = repo.save(caseFile);

		auditEventService.auditCreate("CASE", String.valueOf(saved.getCaseId()), saved);

		return toResponse(saved);
	}

	public List<CaseFileResponse> findAll() {
		log.debug("Retrieving all case files");
		if (SecurityContextUtils.isLawEnforcement()) {
			return repo.findVisibleToLawEnforcement().stream()
					.map(this::toResponse)
					.collect(Collectors.toList());
		}
		if (SecurityContextUtils.isComplianceUser()) {
			String userId = SecurityContextUtils.getCurrentUserId().map(UUID::toString).orElse(null);
			if (userId == null)
				return List.of();
			List<Long> caseIds = documentRepo.findDistinctCaseIdsByUploadedByUserId(userId);
			if (caseIds.isEmpty())
				return List.of();
			return repo.findAllById(caseIds).stream()
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

	@Transactional
	public CaseFileResponse referById(Long caseId, ReferCaseRequest request) {
		log.debug("Referring case file with ID: {} to agency: {}", caseId, request.referredToAgency());
		CaseFile caseFile = repo.findById(caseId)
				.orElseThrow(() -> new ResourceNotFoundException("Case file with ID " + caseId + " not found"));
		enforceCaseAccess(caseId);

		if (request.referredToAgency() == null || request.referredToAgency().trim().isEmpty()) {
			throw new IllegalArgumentException("Referred to agency is required");
		}

		caseFile.setStatus(CaseStatus.REFERRED);
		caseFile.setReferredToAgency(request.referredToAgency());
		caseFile.setReferredAt(Instant.now());

		CaseFile saved = repo.save(caseFile);
		log.info("Referred case file with ID: {} (SAR ID: {}) to agency: {}",
				saved.getCaseId(), saved.getSarId(), saved.getReferredToAgency());

		// Create audit event for refer action
		java.util.Map<String, Object> referMetadata = new java.util.HashMap<>();
		referMetadata.put("referredToAgency", saved.getReferredToAgency());
		referMetadata.put("referredAt", saved.getReferredAt().toString());
		auditEventService.auditAction("CASE", String.valueOf(saved.getCaseId()), "REFER", referMetadata);

		return toResponse(saved);
	}

	@Transactional
	public CaseFileResponse closeById(Long caseId) {
		log.debug("Closing case file with ID: {}", caseId);
		CaseFile caseFile = repo.findById(caseId)
				.orElseThrow(() -> new ResourceNotFoundException("Case file with ID " + caseId + " not found"));
		enforceCaseAccess(caseId);

		caseFile.setStatus(CaseStatus.CLOSED);
		caseFile.setClosedAt(Instant.now());

		CaseFile saved = repo.save(caseFile);
		log.info("Closed case file with ID: {} (SAR ID: {})", saved.getCaseId(), saved.getSarId());

		// Create audit event for close action
		java.util.Map<String, Object> closeMetadata = new java.util.HashMap<>();
		closeMetadata.put("closedAt", saved.getClosedAt().toString());
		auditEventService.auditAction("CASE", String.valueOf(saved.getCaseId()), "CLOSE", closeMetadata);

		return toResponse(saved);
	}

	public CaseFileResponse findById(Long caseId) {
		log.debug("Retrieving case file with ID: {}", caseId);
		CaseFile caseFile = repo.findById(caseId)
				.orElseThrow(() -> new ResourceNotFoundException("Case file with ID " + caseId + " not found"));
		enforceCaseAccess(caseId);
		return toResponse(caseFile);
	}

	private void enforceCaseAccess(Long caseId) {
		if (SecurityContextUtils.isLawEnforcement() && !visibleToLawEnforcementCaseIds().contains(caseId)) {
			throw new ResourceNotFoundException("Case file with ID " + caseId + " not found");
		}
		if (SecurityContextUtils.isComplianceUser() && !visibleToComplianceUserCaseIds().contains(caseId)) {
			throw new ResourceNotFoundException("Case file with ID " + caseId + " not found");
		}
		// ANALYST has full access; unknown roles denied by SecurityConfig
	}

	private Set<Long> visibleToLawEnforcementCaseIds() {
		return repo.findVisibleToLawEnforcement().stream()
				.map(CaseFile::getCaseId)
				.collect(Collectors.toSet());
	}

	private Set<Long> visibleToComplianceUserCaseIds() {
		String userId = SecurityContextUtils.getCurrentUserId().map(UUID::toString).orElse(null);
		if (userId == null)
			return Set.of();
		return documentRepo.findDistinctCaseIdsByUploadedByUserId(userId).stream()
				.collect(Collectors.toSet());
	}

	@Transactional
	public CaseFileResponse updateById(Long caseId, UpdateCaseFileRequest request) {
		log.debug("Updating case file with ID: {}", caseId);
		log.debug("Update request - sarId: {}, status: {}, referredToAgency: {}",
				request.sarId(), request.status(), request.referredToAgency());
		CaseFile caseFile = repo.findById(caseId)
				.orElseThrow(() -> new ResourceNotFoundException("Case file with ID " + caseId + " not found"));
		enforceCaseAccess(caseId);

		// Create a copy of the old case file for audit comparison
		CaseFile oldCaseFile = new CaseFile();
		oldCaseFile.setCaseId(caseFile.getCaseId());
		oldCaseFile.setSarId(caseFile.getSarId());
		oldCaseFile.setStatus(caseFile.getStatus());
		oldCaseFile.setCreatedAt(caseFile.getCreatedAt());
		oldCaseFile.setReferredAt(caseFile.getReferredAt());
		oldCaseFile.setClosedAt(caseFile.getClosedAt());
		oldCaseFile.setReferredToAgency(caseFile.getReferredToAgency());

		boolean updated = false;

		if (request.sarId() != null) {
			repo.findBySarId(request.sarId()).ifPresent(existing -> {
				if (!existing.getCaseId().equals(caseId)) {
					throw new ResourceConflictException("Case file with SAR ID " + request.sarId() + " already exists");
				}
			});
			caseFile.setSarId(request.sarId());
			caseFile.setCtrId(null); // enforce XOR rule at application layer too
			updated = true;
		}

		if (request.ctrId() != null) {
			repo.findByCtrId(request.ctrId()).ifPresent(existing -> {
				if (!existing.getCaseId().equals(caseId)) {
					throw new ResourceConflictException("Case file with CTR ID " + request.ctrId() + " already exists");
				}
			});
			caseFile.setCtrId(request.ctrId());
			caseFile.setSarId(null); // enforce XOR rule
			updated = true;
		}

		if (request.status() != null) {
			log.debug("Updating status from {} to {}", caseFile.getStatus(), request.status());
			caseFile.setStatus(request.status());
			updated = true;
		}

		if (request.referredToAgency() != null) {
			log.debug("Updating referredToAgency from {} to {}", caseFile.getReferredToAgency(),
					request.referredToAgency());
			caseFile.setReferredToAgency(request.referredToAgency());
			updated = true;
		}

		if (!updated) {
			log.warn("No fields to update for case file with ID: {}", caseId);
			return toResponse(caseFile);
		}

		CaseFile saved = repo.save(caseFile);
		log.info("Updated case file with ID: {} (SAR ID: {})", saved.getCaseId(), saved.getSarId());

		// Create audit event for update
		auditEventService.auditUpdate("CASE", String.valueOf(saved.getCaseId()), oldCaseFile, saved);

		return toResponse(saved);
	}

	@Transactional
	public void deleteById(Long caseId) {
		log.debug("Deleting case file with ID: {}", caseId);
		CaseFile caseFile = repo.findById(caseId)
				.orElseThrow(() -> new ResourceNotFoundException("Case file with ID " + caseId + " not found"));
		enforceCaseAccess(caseId);

		// Create audit event before deletion
		auditEventService.auditDelete("CASE", String.valueOf(caseId), caseFile);

		repo.deleteById(caseId);
		log.info("Deleted case file with ID: {}", caseId);
	}

}
