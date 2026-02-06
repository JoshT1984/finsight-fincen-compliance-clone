package com.skillstorm.finsight.compliance_event.models;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "cash_transaction",
  indexes = {
    @Index(name = "idx_txn_subject_time", columnList = "source_subject_id, txn_time"),
    @Index(name = "idx_txn_ext_subject_time", columnList = "external_subject_key, txn_time"),
    @Index(name = "idx_txn_time", columnList = "txn_time")
  }
)
public class CashTransaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "txn_id")
  private Long txnId;

  @Column(name = "source_system", nullable = false, length = 64)
  private String sourceSystem;

  @Column(name = "source_txn_id", length = 64)
  private String sourceTxnId;

  @Column(name = "external_subject_key", length = 128)
  private String externalSubjectKey;

  @Column(name = "source_subject_type", length = 32)
  private String sourceSubjectType;

  @Column(name = "source_subject_id", length = 128)
  private String sourceSubjectId;

  @Column(name = "subject_name", length = 256)
  private String subjectName;

  @Column(name = "txn_time", nullable = false)
  private Instant txnTime;

  @Column(name = "cash_in", nullable = false, precision = 14, scale = 2)
  private BigDecimal cashIn;

  @Column(name = "cash_out", nullable = false, precision = 14, scale = 2)
  private BigDecimal cashOut;

  @Column(name = "currency", nullable = false, length = 3)
  private String currency;

  @Column(name = "channel", nullable = false, length = 32)
  private String channel;

  @Column(name = "location", length = 128)
  private String location;

  @Column(name = "created_at", nullable = false, updatable=false, insertable=false, columnDefinition = "TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3) ")
  private Instant createdAt;

    public CashTransaction() {
    }

    public CashTransaction(Long txnId) {
      this.txnId = txnId;
    }

    public CashTransaction(Long txnId, String sourceSystem, String sourceTxnId, String externalSubjectKey,
                String sourceSubjectType, String sourceSubjectId, String subjectName, Instant txnTime, BigDecimal cashIn,
                BigDecimal cashOut, String currency, String channel, String location, Instant createdAt) {
      this.txnId = txnId;
      this.sourceSystem = sourceSystem;
      this.sourceTxnId = sourceTxnId;
      this.externalSubjectKey = externalSubjectKey;
      this.sourceSubjectType = sourceSubjectType;
      this.sourceSubjectId = sourceSubjectId;
      this.subjectName = subjectName;
      this.txnTime = txnTime;
      this.cashIn = cashIn;
      this.cashOut = cashOut;
      this.currency = currency;
      this.channel = channel;
      this.location = location;
      this.createdAt = createdAt;
    }

  public Long getTxnId() {
    return txnId;
  }

  public void setTxnId(Long txnId) {
    this.txnId = txnId;
  }

  public String getSourceSystem() {
    return sourceSystem;
  }

  public void setSourceSystem(String sourceSystem) {
    this.sourceSystem = sourceSystem;
  }

  public String getSourceTxnId() {
    return sourceTxnId;
  }

  public void setSourceTxnId(String sourceTxnId) {
    this.sourceTxnId = sourceTxnId;
  }

  public String getExternalSubjectKey() {
    return externalSubjectKey;
  }

  public void setExternalSubjectKey(String externalSubjectKey) {
    this.externalSubjectKey = externalSubjectKey;
  }

  public String getSourceSubjectType() {
    return sourceSubjectType;
  }

  public void setSourceSubjectType(String sourceSubjectType) {
    this.sourceSubjectType = sourceSubjectType;
  }

  public String getSourceSubjectId() {
    return sourceSubjectId;
  }

  public void setSourceSubjectId(String sourceSubjectId) {
    this.sourceSubjectId = sourceSubjectId;
  }

  public String getSubjectName() {
    return subjectName;
  }

  public void setSubjectName(String subjectName) {
    this.subjectName = subjectName;
  }

  public Instant getTxnTime() {
    return txnTime;
  }

  public void setTxnTime(Instant txnTime) {
    this.txnTime = txnTime;
  }

  public BigDecimal getCashIn() {
    return cashIn;
  }

  public void setCashIn(BigDecimal cashIn) {
    this.cashIn = cashIn;
  }

  public BigDecimal getCashOut() {
    return cashOut;
  }

  public void setCashOut(BigDecimal cashOut) {
    this.cashOut = cashOut;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }


    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof CashTransaction))
        return false;
      CashTransaction that = (CashTransaction) o;
      return txnId != null && txnId.equals(that.txnId);
    }

    @Override
    public int hashCode() {
      return getClass().hashCode();
    }


    @Override
    public String toString() {
      return "CashTransaction{" +
          "txnId=" + txnId +
          ", sourceSystem='" + sourceSystem + '\'' +
          ", sourceTxnId='" + sourceTxnId + '\'' +
          ", externalSubjectKey='" + externalSubjectKey + '\'' +
          ", sourceSubjectType='" + sourceSubjectType + '\'' +
          ", sourceSubjectId='" + sourceSubjectId + '\'' +
          ", subjectName='" + subjectName + '\'' +
          ", txnTime=" + txnTime +
          ", cashIn=" + cashIn +
          ", cashOut=" + cashOut +
          ", currency='" + currency + '\'' +
          ", channel='" + channel + '\'' +
          ", location='" + location + '\'' +
          ", createdAt=" + createdAt +
          '}';
    }

 
  
}
