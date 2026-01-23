-- ======================================
-- Documents + Cases Service Schema
-- ======================================

CREATE TABLE IF NOT EXISTS case_file (
  case_id BIGSERIAL PRIMARY KEY,

  -- sar_id belongs to SAR service; no FK here.
  sar_id BIGINT NOT NULL UNIQUE,

  status VARCHAR(32) NOT NULL CHECK (status IN ('OPEN','REFERRED','CLOSED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  referred_at TIMESTAMPTZ,
  closed_at TIMESTAMPTZ,
  referred_to_agency VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_case_status_created ON case_file(status, created_at);

CREATE TABLE IF NOT EXISTS case_note (
  note_id BIGSERIAL PRIMARY KEY,
  case_id BIGINT NOT NULL REFERENCES case_file(case_id) ON DELETE CASCADE,

  -- author_user_id belongs to Identity service; no FK here.
  author_user_id UUID NOT NULL,

  note_text TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_case_note_case_created ON case_note(case_id, created_at);

CREATE TABLE IF NOT EXISTS document (
  document_id BIGSERIAL PRIMARY KEY,
  document_type VARCHAR(16) NOT NULL CHECK (document_type IN ('CTR','SAR','CASE')),
  file_name VARCHAR(256) NOT NULL,
  storage_path VARCHAR(512) NOT NULL,
  uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now(),

  -- Link target is external IDs (ctr_id, sar_id) or internal (case_id).
  -- Validation rules:
  -- - CTR documents: ctr_id only
  -- - SAR documents: sar_id required, case_id optional (when case is auto-created)
  -- - CASE documents: case_id only
  ctr_id BIGINT,
  sar_id BIGINT,
  case_id BIGINT REFERENCES case_file(case_id) ON DELETE CASCADE,

  CHECK (
    -- At least one ID must be set
    (ctr_id IS NOT NULL)::int +
    (sar_id IS NOT NULL)::int +
    (case_id IS NOT NULL)::int >= 1
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

CREATE INDEX IF NOT EXISTS idx_document_type_uploaded ON document(document_type, uploaded_at);
CREATE INDEX IF NOT EXISTS idx_document_case ON document(case_id);

CREATE TABLE IF NOT EXISTS audit_event (
  audit_id BIGSERIAL PRIMARY KEY,

  actor_user_id UUID,

  action VARCHAR(64) NOT NULL,
  entity_type VARCHAR(64) NOT NULL,
  entity_id VARCHAR(64) NOT NULL,
  metadata JSONB,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_event(created_at);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_event(entity_type, entity_id);
