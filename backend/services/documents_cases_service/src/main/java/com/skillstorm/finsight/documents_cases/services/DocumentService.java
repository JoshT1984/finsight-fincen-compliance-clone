package com.skillstorm.finsight.documents_cases.services;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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

@Service
public class DocumentService {
	
    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    
    private final DocumentRepository repo;
    private final S3Service s3Service;
    private final CaseFileRepository caseFileRepo;
    private final ComplianceEventServiceClient complianceEventClient;
    
    public DocumentService(DocumentRepository repo, S3Service s3Service, CaseFileRepository caseFileRepo, ComplianceEventServiceClient complianceEventClient) {
    	this.repo = repo;
    	this.s3Service = s3Service;
    	this.caseFileRepo = caseFileRepo;
    	this.complianceEventClient = complianceEventClient;
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
    			document.getCaseId()
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
    	
    	Document saved = repo.save(document);
    	log.info("Created document with ID: {} (type: {}, file: {})", 
    			saved.getDocumentId(), saved.getDocumentType(), saved.getFileName());
    	
    	return toResponse(saved);
    }
    
    public List<DocumentResponse> findAll() {
    	log.debug("Retrieving all documents");
    	return repo.findAll().stream()
    			.map(this::toResponse)
    			.collect(Collectors.toList());
    }
    
    public DocumentResponse findById(Long documentId) {
    	log.debug("Retrieving document with ID: {}", documentId);
    	
    	Document document = repo.findById(documentId)
    			.orElseThrow(() -> new ResourceNotFoundException("Document with ID " + documentId + " not found"));
    	
    	return toResponse(document);
    }
    
    public List<DocumentResponse> findByCtrId(Long ctrId) {
    	log.debug("Retrieving documents for CTR ID: {}", ctrId);
    	return repo.findByCtrId(ctrId).stream()
    			.map(this::toResponse)
    			.collect(Collectors.toList());
    }
    
    public List<DocumentResponse> findBySarId(Long sarId) {
    	log.debug("Retrieving documents for SAR ID: {}", sarId);
    	return repo.findBySarId(sarId).stream()
    			.map(this::toResponse)
    			.collect(Collectors.toList());
    }
    
    public List<DocumentResponse> findByCaseId(Long caseId) {
    	log.debug("Retrieving documents for Case ID: {}", caseId);
    	return repo.findByCaseId(caseId).stream()
    			.map(this::toResponse)
    			.collect(Collectors.toList());
    }
    
    @Transactional
    public DocumentResponse updateById(Long documentId, UpdateDocumentRequest request) {
    	log.debug("Updating document with ID: {}", documentId);
    	log.debug("Update request - documentType: {}, fileName: {}, storagePath: {}", 
    			request.documentType(), request.fileName(), request.storagePath());
    	
    	Document document = repo.findById(documentId)
    			.orElseThrow(() -> new ResourceNotFoundException("Document with ID " + documentId + " not found"));
    	
    	boolean updated = false;
    	
    	if (request.documentType() != null) {
    		log.debug("Updating documentType from {} to {}", document.getDocumentType(), request.documentType());
    		document.setDocumentType(request.documentType());
    		updated = true;
    	}
    	
    	if (request.fileName() != null) {
    		log.debug("Updating fileName from {} to {}", document.getFileName(), request.fileName());
    		document.setFileName(request.fileName());
    		updated = true;
    	}
    	
    	if (request.storagePath() != null) {
    		log.debug("Updating storagePath from {} to {}", document.getStoragePath(), request.storagePath());
    		document.setStoragePath(request.storagePath());
    		updated = true;
    	}
    	
    	if (!updated) {
    		log.warn("No fields to update for document with ID: {}", documentId);
    		return toResponse(document);
    	}
    	
    	Document saved = repo.save(document);
    	log.info("Updated document with ID: {} (type: {}, file: {})", 
    			saved.getDocumentId(), saved.getDocumentType(), saved.getFileName());
    	
    	return toResponse(saved);
    }
    
    @Transactional
    public void deleteById(Long documentId) {
    	log.debug("Deleting document with ID: {}", documentId);
    	
    	// Retrieve document first to get the S3 storage path
    	Document document = repo.findById(documentId)
    			.orElseThrow(() -> new ResourceNotFoundException("Document with ID " + documentId + " not found"));
    	
    	String storagePath = document.getStoragePath();
    	log.info("Deleting document ID: {} with storagePath: {}", documentId, storagePath);
    	
    	try {
    		s3Service.deleteFile(storagePath);
    		log.info("Successfully deleted file from S3 for document ID: {}", documentId);
    	} catch (java.io.IOException e) {
    		log.error("CRITICAL: Failed to delete file from S3 for document ID: {} (storagePath: {}). Error: {}. Continuing with database deletion.", 
    				documentId, storagePath, e.getMessage(), e);

    	}
    	
    	// Delete from database
    	repo.deleteById(documentId);
    	log.info("Deleted document with ID: {} (storagePath: {})", documentId, document.getStoragePath());
    }
    
    public DocumentDownloadUrlResponse getDownloadUrl(Long documentId, Integer expirationMinutes) {
    	log.debug("Generating download URL for document ID: {}", documentId);
    	
    	Document document = repo.findById(documentId)
    			.orElseThrow(() -> new ResourceNotFoundException("Document with ID " + documentId + " not found"));
    	
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
    	
    	try {
    		Document saved = repo.save(document);
    		log.info("Created document with ID: {} (type: {}, file: {}, S3 key: {})", 
    				saved.getDocumentId(), saved.getDocumentType(), saved.getFileName(), saved.getStoragePath());
    		
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
