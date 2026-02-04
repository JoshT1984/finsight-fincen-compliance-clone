package com.skillstorm.finsight.documents_cases.models;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "document")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 16)
    private DocumentType documentType;

    @Column(name = "file_name", nullable = false, length = 256)
    private String fileName;

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "ctr_id")
    private Long ctrId;

    @Column(name = "sar_id")
    private Long sarId;

    @Column(name = "case_id")
    private Long caseId;

    @Column(name = "uploaded_by_user_id", length = 36)
    private String uploadedByUserId;

    public Document() {
    }

    public Document(Long documentId, DocumentType documentType, String fileName, String storagePath, Instant uploadedAt, Long ctrId, Long sarId, Long caseId, String uploadedByUserId) {
        this.documentId = documentId;
        this.documentType = documentType;
        this.fileName = fileName;
        this.storagePath = storagePath;
        this.uploadedAt = uploadedAt;
        this.ctrId = ctrId;
        this.sarId = sarId;
        this.caseId = caseId;
        this.uploadedByUserId = uploadedByUserId;
    }
    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Long getCtrId() {
        return ctrId;
    }

    public void setCtrId(Long ctrId) {
        this.ctrId = ctrId;
    }

    public Long getSarId() {
        return sarId;
    }

    public void setSarId(Long sarId) {
        this.sarId = sarId;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public String getUploadedByUserId() {
        return uploadedByUserId;
    }

    public void setUploadedByUserId(String uploadedByUserId) {
        this.uploadedByUserId = uploadedByUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(documentId, document.documentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(documentId);
    }

    @Override
    public String toString() {
        return "Document{" +
                "documentId=" + documentId +
                ", documentType=" + documentType +
                ", fileName='" + fileName + '\'' +
                ", storagePath='" + storagePath + '\'' +
                ", uploadedAt=" + uploadedAt +
                ", ctrId=" + ctrId +
                ", sarId=" + sarId +
                ", caseId=" + caseId +
                ", uploadedByUserId=" + uploadedByUserId +
                '}';
    }
}
