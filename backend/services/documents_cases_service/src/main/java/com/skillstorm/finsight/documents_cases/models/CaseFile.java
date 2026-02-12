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
@Table(name = "case_file")
public class CaseFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "case_id")
    private Long caseId;

    // Nullable: SAR-backed cases
    @Column(name = "sar_id", unique = true)
    private Long sarId;

    // Nullable: CTR-backed analyst review cases
    @Column(name = "ctr_id", unique = true)
    private Long ctrId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CaseStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "referred_at")
    private Instant referredAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "referred_to_agency", length = 128)
    private String referredToAgency;

    public CaseFile() {}

    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }

    public Long getSarId() { return sarId; }
    public void setSarId(Long sarId) { this.sarId = sarId; }

    public Long getCtrId() { return ctrId; }
    public void setCtrId(Long ctrId) { this.ctrId = ctrId; }

    public CaseStatus getStatus() { return status; }
    public void setStatus(CaseStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getReferredAt() { return referredAt; }
    public void setReferredAt(Instant referredAt) { this.referredAt = referredAt; }

    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }

    public String getReferredToAgency() { return referredToAgency; }
    public void setReferredToAgency(String referredToAgency) { this.referredToAgency = referredToAgency; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaseFile caseFile = (CaseFile) o;
        return Objects.equals(caseId, caseFile.caseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId);
    }
}
