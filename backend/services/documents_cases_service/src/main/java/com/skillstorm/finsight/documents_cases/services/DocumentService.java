package com.skillstorm.finsight.documents_cases.services;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.skillstorm.finsight.documents_cases.dtos.request.CreateDocumentRequest;
import com.skillstorm.finsight.documents_cases.dtos.request.UpdateDocumentRequest;
import com.skillstorm.finsight.documents_cases.dtos.response.DocumentDownloadUrlResponse;
import com.skillstorm.finsight.documents_cases.dtos.response.DocumentResponse;
import com.skillstorm.finsight.documents_cases.exceptions.ResourceNotFoundException;
import com.skillstorm.finsight.documents_cases.models.CaseFile;
import com.skillstorm.finsight.documents_cases.models.CaseStatus;
import com.skillstorm.finsight.documents_cases.models.Document;
import com.skillstorm.finsight.documents_cases.models.DocumentType;
import com.skillstorm.finsight.documents_cases.repositories.CaseFileRepository;
import com.skillstorm.finsight.documents_cases.repositories.DocumentRepository;
import com.skillstorm.finsight.documents_cases.utils.SecurityContextUtils;

@Service
public class DocumentService {
	
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    
    private final DocumentRepository repo;
    private final S3Service s3Service;
    private final CaseFileRepository caseFileRepo;
    private final ComplianceEventServiceClient complianceEventClient;
    private final AuditEventService auditEventService;
    private final DocumentEventPublisher documentEventPublisher;
    
    public DocumentService(DocumentRepository repo, S3Service s3Service, CaseFileRepository caseFileRepo, ComplianceEventServiceClient complianceEventClient, AuditEventService auditEventService, DocumentEventPublisher documentEventPublisher) {
    	this.repo = repo;
    	this.s3Service = s3Service;
    	this.caseFileRepo = caseFileRepo;
    	this.complianceEventClient = complianceEventClient;
    	this.auditEventService = auditEventService;
    	this.documentEventPublisher = documentEventPublisher;
    }
    
    private DocumentResponse toResponse(Document document) {
    	return new DocumentResponse(
    			document.getDocumentId(),
    			document.getDocumentType(),
    			document.getFileName(),
    			document.getStoragePath(),
    			document.getUploadedAt(),
    			document.getCtrId(),
    			document.getSarId(),
    			document.getCaseId(),
    			document.getUploadedByUserId()
    	);
    }
    
    @Transactional
    public DocumentResponse create(CreateDocumentRequest dto) {
    	log.debug("Creating document: {}", dto.fileName());
    	
    	// Validate based on document type
    	// CTR and SAR IDs are optional - will be created if not provided
    	if (dto.documentType() == DocumentType.CTR) {
    		if (dto.sarId() != null || dto.caseId() != null) {
    			throw new IllegalArgumentException("CTR documents can only have ctrId, not sarId or caseId");
    		}
    	} else if (dto.documentType() == DocumentType.SAR) {
    		if (dto.ctrId() != null) {
    			throw new IllegalArgumentException("SAR documents cannot have ctrId");
    		}
    		// caseId is optional for SAR documents - if not provided, auto-create case
    	} else if (dto.documentType() == DocumentType.CASE) {
    		if (dto.caseId() == null) {
    			throw new IllegalArgumentException("CASE documents must have a caseId");
    		}
    		if (dto.ctrId() != null || dto.sarId() != null) {
    			throw new IllegalArgumentException("CASE documents can only have caseId, not ctrId or sarId");
    		}
    		// Validate that the case exists
    		if (!caseFileRepo.existsById(dto.caseId())) {
    			throw new ResourceNotFoundException("Case with ID " + dto.caseId() + " not found");
    		}
    	}
    	
    	// Create CTR/SAR records if IDs not provided
    	Long finalCtrId = dto.ctrId();
    	Long finalSarId = dto.sarId();
    	
    	if (dto.documentType() == DocumentType.CTR && finalCtrId == null) {
    		// Create CTR record
    		finalCtrId = complianceEventClient.createCtrRecord();
    		log.info("Created CTR record with ID: {}", finalCtrId);
    	} else if (dto.documentType() == DocumentType.SAR && finalSarId == null) {
    		// Create SAR record
    		finalSarId = complianceEventClient.createSarRecord();
    		log.info("Created SAR record with ID: {}", finalSarId);
    	}
    	
    	// For SAR documents, auto-create case if caseId not provided
    	Long finalCaseId = dto.caseId();
    	if (dto.documentType() == DocumentType.SAR) {
    		if (finalCaseId == null) {
    			// Check if case already exists for this SAR
    			CaseFile existingCase = caseFileRepo.findBySarId(finalSarId).orElse(null);
    			if (existingCase != null) {
    				finalCaseId = existingCase.getCaseId();
    				log.info("Found existing case ID: {} for SAR ID: {}", finalCaseId, finalSarId);
    			} else {
    				// Auto-create case for SAR
    				CaseFile newCase = new CaseFile();
    				newCase.setSarId(finalSarId);
    				newCase.setStatus(CaseStatus.OPEN);
    				newCase.setCreatedAt(Instant.now());
    				CaseFile savedCase = caseFileRepo.save(newCase);
    				finalCaseId = savedCase.getCaseId();
    				log.info("Auto-created case ID: {} for SAR ID: {}", finalCaseId, finalSarId);
    			}
    		} else {
    			// Validate that the provided case exists
    			if (!caseFileRepo.existsById(finalCaseId)) {
    				throw new ResourceNotFoundException("Case with ID " + finalCaseId + " not found");
    			}
    		}
    	}
    	
    	Document document = new Document();
    	document.setDocumentType(dto.documentType());
    	document.setFileName(dto.fileName());
    	document.setStoragePath(dto.storagePath());
    	document.setUploadedAt(Instant.now());
    	document.setCtrId(finalCtrId);
    	document.setSarId(finalSarId);
    	document.setCaseId(finalCaseId);
    	SecurityContextUtils.getCurrentUserId().map(UUID::toString).ifPresent(document::setUploadedByUserId);

    	Document saved = repo.save(document);
    	log.info("Created document with ID: {} (type: {}, file: {})", 
    			saved.getDocumentId(), saved.getDocumentType(), saved.getFileName());
    	
    	// Create audit event
    	auditEventService.auditCreate("DOCUMENT", String.valueOf(saved.getDocumentId()), saved);
    	
    	// Publish to RabbitMQ for compliance service (CTR/SAR only)
    	documentEventPublisher.publishDocumentUploadEvent(saved);
    	
    	return toResponse(saved);
    }
    
    public List<DocumentResponse> findAll() {
    	log.debug("Retrieving all documents");
    	if (SecurityContextUtils.isComplianceUser()) {
    		String userId = currentUserIdString();
    		if (userId == null) return List.of();
    		return repo.findByUploadedByUserId(userId).stream()
    				.map(this::toResponse)
    				.collect(Collectors.toList());
    	}
    	if (SecurityContextUtils.isLawEnforcement()) {
    		Set<Long> visibleCaseIds = visibleToLawEnforcementCaseIds();
    		Set<Long> visibleSarIds = visibleToLawEnforcementSarIds();
    		List<Document> byCase = repo.findByCaseIdIn(List.copyOf(visibleCaseIds));
    		List<Document> bySar = visibleSarIds.isEmpty() ? List.of() : repo.findBySarIdIn(List.copyOf(visibleSarIds));
    		return java.util.stream.Stream.concat(byCase.stream(), bySar.stream())
    				.distinct()
    				.map(this::toResponse)
    				.collect(Collectors.toList());
    	}
    	if (SecurityContextUtils.isAnalyst()) {
    		return repo.findAll().stream()
    				.map(this::toResponse)
    				.collect(Collectors.toList());
    	}
    	// Unknown or unauthenticated role - deny access
    	log.warn("Document findAll denied: user has no recognized role (ANALYST, LAW_ENFORCEMENT_USER, COMPLIANCE_USER)");
    	return List.of();
    }

    public DocumentResponse findById(Long documentId) {
    	log.debug("Retrieving document with ID: {}", documentId);
    	Document document = repo.findById(documentId)
    			.orElseThrow(() -> new ResourceNotFoundException("Document with ID " + documentId + " not found"));
    	enforceDocumentAccess(document);
    	return toResponse(document);
    }

    public List<DocumentResponse> findByCtrId(Long ctrId) {
    	log.debug("Retrieving documents for CTR ID: {}", ctrId);
    	if (SecurityContextUtils.isLawEnforcement()) {
    		return List.of(); // Law enforcement has no access to CTR documents
    	}
    	if (SecurityContextUtils.isComplianceUser()) {
    		String userId = currentUserIdString();
    		if (userId == null) return List.of();
    		return repo.findByCtrId(ctrId).stream()
    				.filter(d -> userId.equals(d.getUploadedByUserId()))
    				.map(this::toResponse)
    				.collect(Collectors.toList());
    	}
    	if (SecurityContextUtils.isAnalyst()) {
    		return repo.findByCtrId(ctrId).stream()
    				.map(this::toResponse)
    				.collect(Collectors.toList());
    	}
    	return List.of();
    }

    public List<DocumentResponse> findBySarId(Long sarId) {
    	log.debug("Retrieving documents for SAR ID: {}", sarId);
    	if (SecurityContextUtils.isComplianceUser()) {
    		String userId = currentUserIdString();
    		if (userId == null) return List.of();
    		return repo.findBySarId(sarId).stream()
    				.filter(d -> userId.equals(d.getUploadedByUserId()))
    				.map(this::toResponse)
    				.collect(Collectors.toList());
    	}
    	if (SecurityContextUtils.isLawEnforcement()) {
    		if (!visibleToLawEnforcementSarIds().contains(sarId)) {
    			return List.of();
    		}
    		return repo.findBySarId(sarId).stream()
    				.map(this::toResponse)
    				.collect(Collectors.toList());
    	}
    	if (SecurityContextUtils.isAnalyst()) {
    		return repo.findBySarId(sarId).stream()
    				.map(this::toResponse)
    				.collect(Collectors.toList());
    	}
    	return List.of();
    }

    public List<DocumentResponse> findByCaseId(Long caseId) {
    	log.debug("Retrieving documents for Case ID: {}", caseId);
    	if (SecurityContextUtils.isLawEnforcement()) {
    		if (!visibleToLawEnforcementCaseIds().contains(caseId)) {
    			return List.of();
    		}
    		// Include CASE documents (case_id) and SAR documents tied to this case (sar_id = case.sarId)
    		CaseFile caseFile = caseFileRepo.findById(caseId).orElse(null);
    		if (caseFile == null) return List.of();
    		List<Document> byCase = repo.findByCaseId(caseId);
    		List<Document> bySar = repo.findBySarId(caseFile.getSarId());
    		return java.util.stream.Stream.concat(byCase.stream(), bySar.stream())
    				.distinct()
    				.map(this::toResponse)
    				.collect(Collectors.toList());
    	}
    	if (SecurityContextUtils.isComplianceUser()) {
    		String userId = currentUserIdString();
    		if (userId == null) return List.of();
    		CaseFile caseFile = caseFileRepo.findById(caseId).orElse(null);
    		if (caseFile == null) return List.of(); // Case does not exist
    		List<Document> byCase = repo.findByCaseId(caseId);
    		List<Document> bySar = repo.findBySarId(caseFile.getSarId());
    		return java.util.stream.Stream.concat(byCase.stream(), bySar.stream())
    				.distinct()
    				.filter(d -> userId.equals(d.getUploadedByUserId()))
    				.map(this::toResponse)
    				.collect(Collectors.toList());
    	}
    	if (SecurityContextUtils.isAnalyst()) {
    		CaseFile caseFile = caseFileRepo.findById(caseId).orElse(null);
    		if (caseFile == null) return repo.findByCaseId(caseId).stream().map(this::toResponse).collect(Collectors.toList());
    		List<Document> byCase = repo.findByCaseId(caseId);
    		List<Document> bySar = repo.findBySarId(caseFile.getSarId());
    		return java.util.stream.Stream.concat(byCase.stream(), bySar.stream())
    				.distinct()
    				.map(this::toResponse)
    				.collect(Collectors.toList());
    	}
    	return List.of();
    }

    private String currentUserIdString() {
    	return SecurityContextUtils.getCurrentUserId().map(UUID::toString).orElse(null);
    }

    private Set<Long> visibleToLawEnforcementCaseIds() {
    	return caseFileRepo.findVisibleToLawEnforcement().stream()
    			.map(CaseFile::getCaseId)
    			.collect(Collectors.toSet());
    }

    private Set<Long> visibleToLawEnforcementSarIds() {
    	return caseFileRepo.findVisibleToLawEnforcement().stream()
    			.map(CaseFile::getSarId)
    			.collect(Collectors.toSet());
    }

    private void enforceDocumentAccess(Document document) {
    	if (SecurityContextUtils.isComplianceUser()) {
    		String userId = currentUserIdString();
    		if (userId == null || !userId.equals(document.getUploadedByUserId())) {
    			throw new ResourceNotFoundException("Document with ID " + document.getDocumentId() + " not found");
    		}
    		return;
    	}
    	if (SecurityContextUtils.isLawEnforcement()) {
    		Set<Long> visibleCaseIds = visibleToLawEnforcementCaseIds();
    		Set<Long> visibleSarIds = visibleToLawEnforcementSarIds();
    		boolean hasAccess = (document.getCaseId() != null && visibleCaseIds.contains(document.getCaseId()))
    				|| (document.getSarId() != null && visibleSarIds.contains(document.getSarId()));
    		if (!hasAccess) {
    			throw new ResourceNotFoundException("Document with ID " + document.getDocumentId() + " not found");
    		}
    		return;
    	}
    	if (SecurityContextUtils.isAnalyst()) {
    		return; // Full access
    	}
    	// Unknown or unauthenticated role - deny access
    	log.warn("Document access denied for ID {}: user has no recognized role", document.getDocumentId());
    	throw new ResourceNotFoundException("Document with ID " + document.getDocumentId() + " not found");
    }
    
    @Transactional
    public DocumentResponse updateById(Long documentId, UpdateDocumentRequest request) throws IOException {
    	log.debug("Updating document with ID: {}", documentId);
    	log.debug("Update request - documentType: {}, fileName: {}, storagePath: {}, ctrId: {}, sarId: {}, caseId: {}", 
    			request.documentType(), request.fileName(), request.storagePath(), 
    			request.ctrId(), request.sarId(), request.caseId());
    	
    	Document document = repo.findById(documentId)
    			.orElseThrow(() -> new ResourceNotFoundException("Document with ID " + documentId + " not found"));
    	enforceDocumentAccess(document);
    	// Create a copy of the old document for audit comparison
    	Document oldDocument = new Document();
    	oldDocument.setDocumentId(document.getDocumentId());
    	oldDocument.setDocumentType(document.getDocumentType());
    	oldDocument.setFileName(document.getFileName());
    	oldDocument.setStoragePath(document.getStoragePath());
    	oldDocument.setUploadedAt(document.getUploadedAt());
    	oldDocument.setCtrId(document.getCtrId());
    	oldDocument.setSarId(document.getSarId());
    	oldDocument.setCaseId(document.getCaseId());
    	oldDocument.setUploadedByUserId(document.getUploadedByUserId());

    	// Store original values to detect changes that require S3 file move
    	DocumentType originalDocumentType = document.getDocumentType();
    	Long originalCtrId = document.getCtrId();
    	Long originalSarId = document.getSarId();
    	Long originalCaseId = document.getCaseId();
    	String originalStoragePath = document.getStoragePath();
    	
    	boolean updated = false;
    	boolean needsS3Move = false;
    	
    	// Determine new values for IDs
    	Long newCtrId = request.ctrId() != null ? request.ctrId() : originalCtrId;
    	Long newSarId = request.sarId() != null ? request.sarId() : originalSarId;
    	Long newCaseId = request.caseId() != null ? request.caseId() : originalCaseId;
    	DocumentType newDocumentType = request.documentType() != null ? request.documentType() : originalDocumentType;
    	
    	// Validate based on document type (same rules as create)
    	if (newDocumentType == DocumentType.CTR) {
    		if (newSarId != null || newCaseId != null) {
    			throw new IllegalArgumentException("CTR documents can only have ctrId, not sarId or caseId");
    		}
    	} else if (newDocumentType == DocumentType.SAR) {
    		if (newCtrId != null) {
    			throw new IllegalArgumentException("SAR documents cannot have ctrId");
    		}
    		// For SAR documents, validate case exists if caseId is provided (same as create method)
    		// Only validate if caseId is being changed (if unchanged, it was already validated at creation)
    		if (newCaseId != null && !newCaseId.equals(originalCaseId)) {
    			if (!caseFileRepo.existsById(newCaseId)) {
    				throw new ResourceNotFoundException("Case with ID " + newCaseId + " not found");
    			}
    		}
    	} else if (newDocumentType == DocumentType.CASE) {
    		if (newCaseId == null) {
    			throw new IllegalArgumentException("CASE documents must have a caseId");
    		}
    		if (newCtrId != null || newSarId != null) {
    			throw new IllegalArgumentException("CASE documents can only have caseId, not ctrId or sarId");
    		}
    		// Validate that the case exists
    		if (!caseFileRepo.existsById(newCaseId)) {
    			throw new ResourceNotFoundException("Case with ID " + newCaseId + " not found");
    		}
    	}
    	
    	// Check if document type or associated IDs changed (requires S3 file move)
    	if (!newDocumentType.equals(originalDocumentType) || 
    			!java.util.Objects.equals(newCtrId, originalCtrId) ||
    			!java.util.Objects.equals(newSarId, originalSarId) ||
    			!java.util.Objects.equals(newCaseId, originalCaseId)) {
    		needsS3Move = true;
    	}
    	
    	// Update document type
    	if (request.documentType() != null) {
    		log.debug("Updating documentType from {} to {}", document.getDocumentType(), request.documentType());
    		document.setDocumentType(request.documentType());
    		updated = true;
    	}
    	
    	// Update IDs
    	if (request.ctrId() != null) {
    		log.debug("Updating ctrId from {} to {}", document.getCtrId(), request.ctrId());
    		document.setCtrId(request.ctrId());
    		updated = true;
    	}
    	
    	if (request.sarId() != null) {
    		log.debug("Updating sarId from {} to {}", document.getSarId(), request.sarId());
    		document.setSarId(request.sarId());
    		updated = true;
    	}
    	
    	if (request.caseId() != null) {
    		log.debug("Updating caseId from {} to {}", document.getCaseId(), request.caseId());
    		document.setCaseId(request.caseId());
    		updated = true;
    	}
    	
    	// Update file name
    	if (request.fileName() != null) {
    		log.debug("Updating fileName from {} to {}", document.getFileName(), request.fileName());
    		document.setFileName(request.fileName());
    		updated = true;
    	}
    	
    	// Handle S3 file move if document type or associated IDs changed
    	if (needsS3Move && originalStoragePath != null && !originalStoragePath.isEmpty()) {
    		// Generate new S3 key based on updated document type and IDs
    		String newS3Key = generateS3Key(newDocumentType, document.getFileName(), newCtrId, newSarId, newCaseId);
    		
    		log.info("Moving S3 file from '{}' to '{}' due to document type/ID change", originalStoragePath, newS3Key);
    		try {
    			String movedKey = s3Service.copyFile(originalStoragePath, newS3Key);
    			document.setStoragePath(movedKey);
    			log.info("Successfully moved S3 file to: {}", movedKey);
    		} catch (IOException e) {
    			log.error("Failed to move S3 file during document update", e);
    			throw new IOException("Failed to move document in S3: " + e.getMessage(), e);
    		}
    	} else if (request.storagePath() != null) {
    		// Manual storage path update (not recommended, but allowed)
    		log.debug("Updating storagePath from {} to {}", document.getStoragePath(), request.storagePath());
    		document.setStoragePath(request.storagePath());
    		updated = true;
    	}
    	
    	if (!updated && !needsS3Move) {
    		log.warn("No fields to update for document with ID: {}", documentId);
    		return toResponse(document);
    	}
    	
    	Document saved = repo.save(document);
    	log.info("Updated document with ID: {} (type: {}, file: {})", 
    			saved.getDocumentId(), saved.getDocumentType(), saved.getFileName());
    	
    	// Create audit event for update
    	auditEventService.auditUpdate("DOCUMENT", String.valueOf(saved.getDocumentId()), oldDocument, saved);
    	
    	return toResponse(saved);
    }
    
    @Transactional
    public void deleteById(Long documentId) {
    	log.debug("Deleting document with ID: {}", documentId);
    	
    	// Retrieve document first to get the S3 storage path
    	Document document = repo.findById(documentId)
    			.orElseThrow(() -> new ResourceNotFoundException("Document with ID " + documentId + " not found"));
    	enforceDocumentAccess(document);
    	String storagePath = document.getStoragePath();
    	log.info("Deleting document ID: {} with storagePath: {}", documentId, storagePath);
    	
    	try {
    		s3Service.deleteFile(storagePath);
    		log.info("Successfully deleted file from S3 for document ID: {}", documentId);
    	} catch (java.io.IOException e) {
    		log.error("CRITICAL: Failed to delete file from S3 for document ID: {} (storagePath: {}). Error: {}. Continuing with database deletion.", 
    				documentId, storagePath, e.getMessage(), e);

    	}
    	
    	// Create audit event before deletion
    	auditEventService.auditDelete("DOCUMENT", String.valueOf(documentId), document);
    	
    	// Delete from database
    	repo.deleteById(documentId);
    	log.info("Deleted document with ID: {} (storagePath: {})", documentId, document.getStoragePath());
    }
    
    public DocumentDownloadUrlResponse getDownloadUrl(Long documentId, Integer expirationMinutes) {
    	log.debug("Generating download URL for document ID: {}", documentId);
    	
    	Document document = repo.findById(documentId)
    			.orElseThrow(() -> new ResourceNotFoundException("Document with ID " + documentId + " not found"));
    	enforceDocumentAccess(document);
    	int expiration = expirationMinutes != null ? expirationMinutes : 15;
    	String downloadUrl = s3Service.generateDownloadUrl(document.getStoragePath(), expiration);
    	
    	java.time.Instant expiresAt = java.time.Instant.now().plus(java.time.Duration.ofMinutes(expiration));
    	
    	log.info("Generated download URL for document ID: {} (expires in {} minutes)", documentId, expiration);
    	
    	return new DocumentDownloadUrlResponse(
    			document.getDocumentId(),
    			document.getFileName(),
    			downloadUrl,
    			expiresAt
    	);
    }
    
    /**
     * Uploads a file to S3 and creates a document record.
     * 
     * @param file The multipart file to upload
     * @param documentType The type of document
     * @param ctrId Optional CTR ID
     * @param sarId Optional SAR ID
     * @param caseId Optional Case ID
     * @return The created document response
     * @throws IOException If there's an error uploading the file
     */
    @Transactional
    public DocumentResponse upload(MultipartFile file, DocumentType documentType, Long ctrId, Long sarId, Long caseId) throws IOException {
    	log.debug("Uploading document: {} (type: {})", file.getOriginalFilename(), documentType);
    	
    	// Validate based on document type and workflow requirements:
    	// - CTR documents: ctrId optional (will be created if not provided), no sarId or caseId
    	// - SAR documents: sarId optional (will be created if not provided), caseId optional (will be auto-created)
    	// - CASE documents: must have caseId only
    	if (documentType == DocumentType.CTR) {
    		if (sarId != null || caseId != null) {
    			throw new IllegalArgumentException("CTR documents can only have ctrId, not sarId or caseId");
    		}
    		// Create CTR record if ctrId not provided
    		if (ctrId == null) {
    			ctrId = complianceEventClient.createCtrRecord();
    			log.info("Created CTR record with ID: {}", ctrId);
    		}
    	} else if (documentType == DocumentType.SAR) {
    		if (ctrId != null) {
    			throw new IllegalArgumentException("SAR documents cannot have ctrId");
    		}
    		// Create SAR record if sarId not provided
    		if (sarId == null) {
    			sarId = complianceEventClient.createSarRecord();
    			log.info("Created SAR record with ID: {}", sarId);
    		}
    		// caseId is optional for SAR documents - if not provided, auto-create case
    		if (caseId == null) {
    			// Check if case already exists for this SAR
    			CaseFile existingCase = caseFileRepo.findBySarId(sarId).orElse(null);
    			if (existingCase != null) {
    				caseId = existingCase.getCaseId();
    				log.info("Found existing case ID: {} for SAR ID: {}", caseId, sarId);
    			} else {
    				// Auto-create case for SAR
    				CaseFile newCase = new CaseFile();
    				newCase.setSarId(sarId);
    				newCase.setStatus(CaseStatus.OPEN);
    				newCase.setCreatedAt(Instant.now());
    				CaseFile savedCase = caseFileRepo.save(newCase);
    				caseId = savedCase.getCaseId();
    				log.info("Auto-created case ID: {} for SAR ID: {}", caseId, sarId);
    			}
    		} else {
    			// Validate that the provided case exists
    			if (!caseFileRepo.existsById(caseId)) {
    				throw new ResourceNotFoundException("Case with ID " + caseId + " not found");
    			}
    		}
    	} else if (documentType == DocumentType.CASE) {
    		if (caseId == null) {
    			throw new IllegalArgumentException("CASE documents must have a caseId");
    		}
    		if (ctrId != null || sarId != null) {
    			throw new IllegalArgumentException("CASE documents can only have caseId, not ctrId or sarId");
    		}
    		// Validate that the case exists
    		if (!caseFileRepo.existsById(caseId)) {
    			throw new ResourceNotFoundException("Case with ID " + caseId + " not found");
    		}
    	}
    	
    	// Validate file is not empty
    	if (file.isEmpty()) {
    		throw new IllegalArgumentException("File cannot be empty");
    	}
    	
    	// Generate S3 key based on document type and associated ID
    	String s3Key = generateS3Key(documentType, file.getOriginalFilename(), ctrId, sarId, caseId);
    	
    	// Upload file to S3
    	String contentType = file.getContentType();
    	if (contentType == null || contentType.isEmpty()) {
    		contentType = "application/octet-stream";
    	}
    	
    	s3Service.uploadFile(file, s3Key, contentType);
    	
    	// Create document record - if this fails, clean up the S3 file
    	Document document = new Document();
    	document.setDocumentType(documentType);
    	document.setFileName(file.getOriginalFilename());
    	document.setStoragePath(s3Key);
    	document.setUploadedAt(Instant.now());
    	document.setCtrId(ctrId);
    	document.setSarId(sarId);
    	document.setCaseId(caseId);
    	SecurityContextUtils.getCurrentUserId().map(UUID::toString).ifPresent(document::setUploadedByUserId);

    	try {
    		Document saved = repo.save(document);
    		log.info("Created document with ID: {} (type: {}, file: {}, S3 key: {})", 
    				saved.getDocumentId(), saved.getDocumentType(), saved.getFileName(), saved.getStoragePath());
    		
    		// Create audit event for upload
    		java.util.Map<String, Object> uploadMetadata = new java.util.HashMap<>();
    		uploadMetadata.put("fileName", file.getOriginalFilename());
    		uploadMetadata.put("fileSize", file.getSize());
    		uploadMetadata.put("contentType", contentType);
    		uploadMetadata.put("s3Key", s3Key);
    		auditEventService.auditAction("DOCUMENT", String.valueOf(saved.getDocumentId()), "UPLOAD", uploadMetadata);
    		
    		// Publish to RabbitMQ for compliance service (CTR/SAR only)
    		documentEventPublisher.publishDocumentUploadEvent(saved);
    		
    		return toResponse(saved);
    	} catch (RuntimeException e) {
    		// If database save fails (constraint violation, etc.), clean up the S3 file to avoid orphaned files
    		log.error("Failed to save document record after S3 upload (S3 key: {}). Cleaning up S3 file.", s3Key, e);
    		try {
    			s3Service.deleteFile(s3Key);
    			log.info("Successfully cleaned up orphaned S3 file: {}", s3Key);
    		} catch (IOException cleanupException) {
    			log.error("Failed to clean up S3 file after database save failure: {}", s3Key, cleanupException);
    			// Continue to throw the original exception
    		}
    		throw e;
    	}
    }
    
    /**
     * Generates an S3 key for storing the document.
     * Format: {documentType}/{id}/{timestamp}-{filename}
     */
    private String generateS3Key(DocumentType documentType, String originalFilename, Long ctrId, Long sarId, Long caseId) {
    	String prefix;
    	Long id;
    	
    	if (ctrId != null) {
    		prefix = "ctr";
    		id = ctrId;
    	} else if (sarId != null) {
    		prefix = "sar";
    		id = sarId;
    	} else {
    		prefix = "case";
    		id = caseId;
    	}
    	
    	// Sanitize filename (remove path separators and special chars)
    	String sanitizedFilename = originalFilename != null 
    			? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_")
    			: "document";
    	
    	// Add timestamp to ensure uniqueness
    	String timestamp = String.valueOf(Instant.now().toEpochMilli());
    	
    	return String.format("%s/%d/%s-%s", prefix, id, timestamp, sanitizedFilename);
    }

}
