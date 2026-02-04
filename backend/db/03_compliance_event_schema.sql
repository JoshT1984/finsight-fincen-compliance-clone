-- ============================================================
-- MySQL 8.0+ (InnoDB) - Instant-friendly
-- TIMESTAMP(3) for millisecond precision
-- Trigger-free (JDBC-safe): uses composite FK guardrails instead
-- ============================================================

CREATE DATABASE IF NOT EXISTS compliance_event;
USE compliance_event;

CREATE DATABASE IF NOT EXISTS compliance_event;
USE compliance_event;

CREATE TABLE IF NOT EXISTS suspect_snapshot_at_time_of_event (
  snapshot_id BIGINT NOT NULL AUTO_INCREMENT,
  suspect_id BIGINT NOT NULL,
  last_known_alias VARCHAR(256),
  last_known_address JSON,
  suspect_minimal JSON,
  captured_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (snapshot_id),
  KEY idx_suspect_snapshot_suspect (suspect_id, captured_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS compliance_event (
  event_id BIGINT NOT NULL AUTO_INCREMENT,

  event_type VARCHAR(16) NOT NULL,
  source_system VARCHAR(64) NOT NULL,
  source_entity_id VARCHAR(64) NOT NULL,

  external_subject_key VARCHAR(128),

  source_subject_type VARCHAR(32),
  source_subject_id VARCHAR(128),

  suspect_snapshot_id BIGINT NULL,

  event_time TIMESTAMP(3) NOT NULL,

  total_amount DECIMAL(14,2) NULL,

  status VARCHAR(32) NULL,
  severity_score INT NULL,

  correlation_id VARCHAR(128) NULL,
  idempotency_key VARCHAR(128) NULL,

  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  PRIMARY KEY (event_id),

  UNIQUE KEY uk_event_id_type (event_id, event_type),
  UNIQUE KEY uk_event_source_entity (source_system, source_entity_id),
  UNIQUE KEY uk_event_idempotency (source_system, idempotency_key),

  KEY idx_event_type_time (event_type, event_time),
  KEY idx_event_source (source_system, source_entity_id),
  KEY idx_event_suspect (suspect_snapshot_id),
  KEY idx_event_subject (source_subject_type, source_subject_id, event_time),
  KEY idx_event_created (created_at),

  CONSTRAINT fk_event_suspect_snapshot
    FOREIGN KEY (suspect_snapshot_id)
    REFERENCES suspect_snapshot_at_time_of_event(snapshot_id)
    ON DELETE SET NULL,

  CONSTRAINT chk_event_type
    CHECK (event_type IN ('CTR','SAR')),

  CONSTRAINT chk_source_subject_type
    CHECK (
      source_subject_type IN ('SUSPECT','CUSTOMER','ACCOUNT')
      OR source_subject_type IS NULL
    ),

  CONSTRAINT chk_total_amount_nullable_nonneg
    CHECK (total_amount IS NULL OR total_amount >= 0),

  CONSTRAINT chk_status_by_event_type
    CHECK (
      status IS NULL
      OR (event_type = 'CTR' AND status IN ('CREATED','FILED'))
      OR (event_type = 'SAR' AND status IN ('DRAFT','SUBMITTED'))
    ),

  CONSTRAINT chk_severity_score_sar_only
    CHECK (
      severity_score IS NULL
      OR (event_type = 'SAR' AND severity_score BETWEEN 0 AND 100)
    )
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS compliance_event_ctr_detail (
  event_id BIGINT NOT NULL,
  event_type VARCHAR(16) NOT NULL DEFAULT 'CTR',

  customer_name VARCHAR(128) NOT NULL,
  transaction_time TIMESTAMP(3) NOT NULL,

  ctr_form_data JSON NOT NULL,

  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  PRIMARY KEY (event_id),

  KEY idx_ctr_detail_created (created_at),
  KEY idx_ctr_detail_tx_time (transaction_time),

  CONSTRAINT chk_ctr_detail_event_type
    CHECK (event_type = 'CTR'),

  CONSTRAINT fk_ctr_detail_event
    FOREIGN KEY (event_id, event_type)
    REFERENCES compliance_event(event_id, event_type)
    ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS compliance_event_sar_detail (
  event_id BIGINT NOT NULL,
  event_type VARCHAR(16) NOT NULL DEFAULT 'SAR',

  narrative TEXT,

  activity_start TIMESTAMP(3) NULL,
  activity_end TIMESTAMP(3) NULL,

  form_data JSON NOT NULL,

  submitted_at TIMESTAMP(3) NULL,

  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  PRIMARY KEY (event_id),

  KEY idx_sar_detail_created (created_at),
  KEY idx_sar_detail_submitted (submitted_at),
  KEY idx_sar_detail_activity (activity_start),

  CONSTRAINT chk_sar_detail_event_type
    CHECK (event_type = 'SAR'),

  CONSTRAINT fk_sar_detail_event
    FOREIGN KEY (event_id, event_type)
    REFERENCES compliance_event(event_id, event_type)
    ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS audit_action (
  audit_id BIGINT NOT NULL AUTO_INCREMENT,

  event_id BIGINT NULL,

  actor_user_id CHAR(36) NULL,
  actor_role VARCHAR(64) NULL,

  action VARCHAR(64) NOT NULL,

  metadata JSON NOT NULL,

  correlation_id VARCHAR(128) NULL,
  idempotency_key VARCHAR(128) NULL,

  created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  PRIMARY KEY (audit_id),

  KEY idx_audit_created (created_at),
  KEY idx_audit_event (event_id, created_at),

  UNIQUE KEY uk_audit_idempotency (idempotency_key),

  CONSTRAINT fk_audit_event
    FOREIGN KEY (event_id)
    REFERENCES compliance_event(event_id)
    ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS compliance_event_link (
  from_event_id BIGINT NOT NULL,
  to_event_id BIGINT NOT NULL,

  link_type VARCHAR(32) NOT NULL,

  evidence_snapshot JSON NOT NULL,

  linked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

  PRIMARY KEY (from_event_id, to_event_id, link_type),

  KEY idx_event_link_from (from_event_id, linked_at),
  KEY idx_event_link_to (to_event_id, linked_at),

  CONSTRAINT chk_link_type
    CHECK (link_type IN ('SAR_SUPPORTS_CTR','CTR_SUPPORTS_SAR','RELATED')),

  CONSTRAINT chk_no_self_link
    CHECK (from_event_id <> to_event_id),

  CONSTRAINT fk_link_from_event
    FOREIGN KEY (from_event_id)
    REFERENCES compliance_event(event_id)
    ON DELETE CASCADE,

  CONSTRAINT fk_link_to_event
    FOREIGN KEY (to_event_id)
    REFERENCES compliance_event(event_id)
    ON DELETE CASCADE
) ENGINE=InnoDB;
