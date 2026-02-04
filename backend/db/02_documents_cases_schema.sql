-- ======================================
-- Documents + Cases Service Schema
-- ======================================

CREATE DATABASE IF NOT EXISTS documents_cases;
USE documents_cases;

CREATE TABLE IF NOT EXISTS case_file (
  case_id BIGINT AUTO_INCREMENT PRIMARY KEY,

  -- sar_id belongs to SAR service; no FK here.
  sar_id BIGINT NOT NULL UNIQUE,

  status VARCHAR(32) NOT NULL CHECK (status IN ('OPEN','REFERRED','CLOSED')),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  referred_at TIMESTAMP NULL,
  closed_at TIMESTAMP NULL,
  referred_to_agency VARCHAR(128)
);

CREATE INDEX idx_case_status_created ON case_file(status, created_at);

CREATE TABLE IF NOT EXISTS case_note (
  note_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  case_id BIGINT NOT NULL,
  FOREIGN KEY (case_id) REFERENCES case_file(case_id) ON DELETE CASCADE,

  -- author_user_id belongs to Identity service; no FK here.
  author_user_id VARCHAR(36) NOT NULL,

  note_text TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_case_note_case_created ON case_note(case_id, created_at);

CREATE TABLE IF NOT EXISTS document (
  document_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  document_type VARCHAR(16) NOT NULL CHECK (document_type IN ('CTR','SAR','CASE')),
  file_name VARCHAR(256) NOT NULL,
  storage_path VARCHAR(512) NOT NULL,
  uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  uploaded_by_user_id VARCHAR(36) NULL,

  -- Link target is external IDs (ctr_id, sar_id) or internal (case_id).
  -- Validation rules:
  -- - CTR documents: ctr_id only
  -- - SAR documents: sar_id required, case_id optional (when case is auto-created)
  -- - CASE documents: case_id only
  ctr_id BIGINT,
  sar_id BIGINT,
  case_id BIGINT,
  FOREIGN KEY (case_id) REFERENCES case_file(case_id) ON DELETE CASCADE,

  CHECK (
    -- At least one ID must be set
    (CASE WHEN ctr_id IS NOT NULL THEN 1 ELSE 0 END +
     CASE WHEN sar_id IS NOT NULL THEN 1 ELSE 0 END +
     CASE WHEN case_id IS NOT NULL THEN 1 ELSE 0 END) >= 1
    AND
    -- CTR documents: only ctr_id can be set
    (document_type != 'CTR' OR (ctr_id IS NOT NULL AND sar_id IS NULL AND case_id IS NULL))
    AND
    -- CASE documents: only case_id can be set
    (document_type != 'CASE' OR (case_id IS NOT NULL AND ctr_id IS NULL AND sar_id IS NULL))
    AND
    -- SAR documents: sar_id required, ctr_id not allowed
    (document_type != 'SAR' OR (sar_id IS NOT NULL AND ctr_id IS NULL))
  )
);

CREATE INDEX idx_document_type_uploaded ON document(document_type, uploaded_at);
CREATE INDEX idx_document_case ON document(case_id);
CREATE INDEX idx_document_uploaded_by ON document(uploaded_by_user_id);

CREATE TABLE IF NOT EXISTS audit_event (
  audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,

  actor_user_id VARCHAR(36),

  action VARCHAR(64) NOT NULL,
  entity_type VARCHAR(64) NOT NULL,
  entity_id VARCHAR(64) NOT NULL,
  metadata JSON,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_created ON audit_event(created_at);
CREATE INDEX idx_audit_entity ON audit_event(entity_type, entity_id);
