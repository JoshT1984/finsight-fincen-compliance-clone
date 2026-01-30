package com.skillstorm.finsight.documents_cases.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.skillstorm.finsight.documents_cases.dtos.DocumentUploadEvent;
import com.skillstorm.finsight.documents_cases.models.Document;
import com.skillstorm.finsight.documents_cases.models.DocumentType;

/**
 * Presigned URL expiration in minutes. Consumer has this long to fetch the document.
 */
@Service
public class DocumentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DocumentEventPublisher.class);
    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 60;

    private final RabbitTemplate rabbitTemplate;
    private final S3Service s3Service;

    @Value("${finsight.rabbitmq.queues.ctr-events:finsight.ctr.events}")
    private String ctrEventsQueue;

    @Value("${finsight.rabbitmq.queues.sar-events:finsight.sar.events}")
    private String sarEventsQueue;

    @Value("${aws.s3.bucket:finsight-documents-2026}")
    private String bucketName;

    public DocumentEventPublisher(RabbitTemplate rabbitTemplate, S3Service s3Service) {
        this.rabbitTemplate = rabbitTemplate;
        this.s3Service = s3Service;
    }

    /**
     * Publishes a document upload event to the appropriate queue for compliance service.
     * CTR documents go to finsight.ctr.events; SAR documents go to finsight.sar.events.
     *
     * <p>Includes a presigned S3 download URL so the consumer can fetch the document directly
     * for extraction. If URL generation fails, the event is still published with downloadUrl=null;
     * the consumer can call GET /api/documents/{id}/download-url for a fresh URL.
     */
    public void publishDocumentUploadEvent(Document document) {
        if (document.getDocumentType() != DocumentType.CTR && document.getDocumentType() != DocumentType.SAR) {
            return;
        }

        String downloadUrl = null;
        if (document.getStoragePath() != null && !document.getStoragePath().isEmpty()) {
            try {
                downloadUrl = s3Service.generateDownloadUrl(document.getStoragePath(), PRESIGNED_URL_EXPIRATION_MINUTES);
            } catch (Exception e) {
                log.warn("Could not generate presigned URL for document {} (storagePath: {}). Consumer can use documents API. Error: {}",
                    document.getDocumentId(), document.getStoragePath(), e.getMessage());
            }
        }

        DocumentUploadEvent event = new DocumentUploadEvent(
            document.getDocumentId(),
            document.getDocumentType(),
            document.getCtrId(),
            document.getSarId(),
            document.getCaseId(),
            document.getFileName(),
            document.getStoragePath(),
            bucketName,
            downloadUrl,
            document.getUploadedAt()
        );

        String queue = document.getDocumentType() == DocumentType.CTR ? ctrEventsQueue : sarEventsQueue;
        try {
            rabbitTemplate.convertAndSend(queue, event);
            log.info("Published {} document upload event to queue {}: documentId={}, downloadUrl={}",
                document.getDocumentType(), queue, document.getDocumentId(), downloadUrl != null ? "included" : "null");
        } catch (Exception e) {
            log.error("Failed to publish document upload event to queue {}: documentId={}",
                queue, document.getDocumentId(), e);
            throw new RuntimeException("Failed to publish document event: " + e.getMessage(), e);
        }
    }
}
